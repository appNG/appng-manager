package org.appng.application.manager.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.appng.api.Environment;
import org.appng.api.RequestUtil;
import org.appng.api.model.Application;
import org.appng.api.model.Site;
import org.appng.api.model.Site.SiteState;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import lombok.AllArgsConstructor;
import lombok.Data;

@RestController
public class Monitor {

	private String sharedSecret;

	public Monitor(@Value("${clusterStateBearerToken:${platform.sharedSecret}}") String sharedSecret) {
		this.sharedSecret = sharedSecret;
	}

	@GetMapping(path = "/monitor", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<Map<String, SiteInfo>> getState(Environment env, Site site,
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) List<String> auths) {

		Boolean useBearer = site.getProperties().getBoolean("monitorUseBearer", true);
		if (useBearer && (null == auths || !auths.contains("Bearer " + sharedSecret))) {
			return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
		}

		Map<String, SiteInfo> siteInfos = new HashMap<>();
		RequestUtil.getSiteNames(env).stream().map(s -> RequestUtil.getSiteByName(env, s)).forEach(s -> {
			Map<String, ApplicationInfo> applicationInfos = new HashMap<>();
			for (Application a : s.getApplications()) {
				ApplicationInfo appInfo = new ApplicationInfo(a.getPackageVersion(), a.getDescription(), a.isHidden(),
						a.isPrivileged(), a.isFileBased());
				applicationInfos.put(a.getName(), appInfo);
			}
			OffsetDateTime startup = OffsetDateTime.ofInstant(s.getStartupTime().toInstant(), ZoneId.systemDefault());
			Duration uptime = Duration.between(startup.toLocalDateTime(), LocalDateTime.now());
			SiteInfo siteInfo = new SiteInfo(s.getState(), s.getHost(), s.getDomain(), startup, uptime,
					applicationInfos);
			siteInfos.put(s.getName(), siteInfo);
		});
		return new ResponseEntity<>(siteInfos, HttpStatus.OK);
	}

	@Data
	@AllArgsConstructor
	class SiteInfo {
		SiteState state;
		String host;
		String domain;
		OffsetDateTime startupTime;
		Duration uptimeSeconds;
		Map<String, ApplicationInfo> applications;
	}

	@Data
	@AllArgsConstructor
	class ApplicationInfo {
		String version;
		String description;
		boolean hidden;
		boolean privileged;
		boolean filebased;
	}

}
