package org.appng.application.manager;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

@Configuration
//@EnableCaching
public class Manager {
	
	public Manager() {
		System.err.println("###");
	}
	
//	@Bean
//	public CacheManager cacheManager() {
//		return new ConcurrentMapCacheManager();
//	}

}
