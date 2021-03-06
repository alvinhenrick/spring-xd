/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.dirt.plugins.job;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.job.SimpleJob;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.xd.dirt.server.AbstractAdminMainIntegrationTests;
import org.springframework.xd.dirt.server.SingleNodeMain;
import org.springframework.xd.dirt.server.SingleNodeServer;
import org.springframework.xd.dirt.server.options.SingleNodeOptions;


/**
 * 
 * @author Glenn Renfro
 * @author Gunnar Hillert
 * 
 */
public class JobRepoTests extends AbstractAdminMainIntegrationTests {


	private static final String SIMPLE_JOB_NAME = "foobar";

	private static final String REPOSITORY_LOCATION = "../data";

	private volatile SingleNodeServer streamServer;

	@Before
	public void setUp() throws Exception {
		File file = new File(REPOSITORY_LOCATION);
		if (file.exists()) {
			assertTrue(file.isDirectory());
			file.delete();
		}
		SingleNodeOptions opts = SingleNodeMain.parseOptions(new String[] { "--httpPort", "0", "--transport", "local",
			"--store",
			"memory", "--analytics", "memory" });
		streamServer = SingleNodeMain.launchSingleNodeServer(opts);

	}

	@After
	public void teardown() throws Exception {
		streamServer.getAdminServer().stop();

		File file = new File(REPOSITORY_LOCATION);
		if (file.exists() && file.isDirectory()) {
			file.delete();
		}
	}

	@Test
	public void checkThatRepoTablesAreCreated() throws Exception {
		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext(
				"classpath:/META-INF/spring-xd/batch/batch.xml");

		DataSource source = applicationContext.getBean("dataSource", DataSource.class);
		JdbcTemplate jdbcTemplate = new JdbcTemplate(source);
		int count = jdbcTemplate.queryForObject(
				"select count(*) from INFORMATION_SCHEMA.system_tables  WHERE TABLE_NAME LIKE 'BATCH_%'",
				Integer.class).intValue();
		assertEquals("The number of batch tables returned from hsqldb did not match.", count, 9);
		applicationContext.close();
	}

	@Test
	public void checkThatContainerHasRepo() throws Exception {
		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext(
				"classpath:/META-INF/spring-xd/batch/batch.xml");

		JobRepository repo = applicationContext.getBean("jobRepository", JobRepository.class);
		JobLauncher launcher = applicationContext.getBean("jobLauncher", JobLauncher.class);
		Job job = new SimpleJob(SIMPLE_JOB_NAME);
		try {
			launcher.run(job, new JobParameters());
		}
		catch (Exception ex) {
			// we can ignore this. Just want to create a fake job instance.
		}
		assertTrue(repo.isJobInstanceExists(SIMPLE_JOB_NAME, new JobParameters()));
		applicationContext.close();
	}
}
