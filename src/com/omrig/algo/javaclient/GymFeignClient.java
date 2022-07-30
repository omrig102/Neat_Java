package com.omrig.algo.javaclient;

import feign.Headers;
import feign.Param;
import feign.RequestLine;
import feign.Response;
import lombok.AllArgsConstructor;
import lombok.Data;

public interface GymFeignClient {

	@RequestLine("GET /v1/envs/")
	Response listEnvs();
	
	
	@RequestLine("POST /v1/envs/")
	@Headers("Content-Type: application/json")
    Response createEnv(CreateEnvRequest body);
	
	@RequestLine("POST /v1/envs/{instanceId}/close/")
	@Headers("Content-Type: application/json")
	Response closeEnv(@Param("instanceId") String instanceId,CloseEnvRequest body);
	
	@RequestLine("POST /v1/envs/{instanceId}/reset/")
	@Headers("Content-Type: application/json")
	Response resetEnv(@Param("instanceId") String instanceId,ResetEnvRequest body);
	
	@RequestLine("POST /v1/envs/{instanceId}/step/")
	@Headers("Content-Type: application/json")
	Response stepEnv(@Param("instanceId") String instanceId,StepEnvRequest body);
	
	@RequestLine("GET /v1/envs/{instanceId}/action_space/sample/")
	@Headers("Content-Type: application/json")
	Response sampleAction(@Param("instanceId") String instanceId);
	
	
	@RequestLine("GET /v1/envs/{instanceId}/action_space/")
	@Headers("Content-Type: application/json")
	Response getActionSpace(@Param("instanceId") String instanceId);
	
	@RequestLine("GET /v1/envs/{instanceId}/observation_space/")
	@Headers("Content-Type: application/json")
	Response getObservationSpace(@Param("instanceId") String instanceId);
	
	
	@RequestLine("POST /v1/envs/{instanceId}/monitor/start/")
	@Headers("Content-Type: application/json")
	Response startMonitor(@Param("instanceId") String instanceId,StartMonitorRequest body);
	
	
	@RequestLine("POST /v1/envs/{instanceId}/monitor/close/")
	@Headers("Content-Type: application/json")
	Response closeMonitor(@Param("instanceId") String instanceId,CloseMonitorRequest body);
	
	
	@RequestLine("POST /v1/upload/")
	@Headers("Content-Type: application/json")
	Response upload(uploadRequest body);
	
	@RequestLine("POST /v1/shutdown/")
	@Headers("Content-Type: application/json")
	Response shutDownServer();
	
	@AllArgsConstructor
	@Data
	public class CreateEnvRequest {
		private String env_id;
	}
	
	@AllArgsConstructor
	@Data
	public class CloseEnvRequest {
		private String instance_id;
	}
	
	@AllArgsConstructor
	@Data
	public class ResetEnvRequest {
		private String instance_id;
	}
	
	@AllArgsConstructor
	@Data
	public class StepEnvRequest {
		private String instance_id;
		private Object action;
		private boolean render;
	}
	
	
	@AllArgsConstructor
	@Data
	public class StartMonitorRequest {
		private String instance_id;
		private boolean force;
		private boolean resume;
		private String directory;
	}
	
	
	@AllArgsConstructor
	@Data
	public class CloseMonitorRequest {
		private String instance_id;
	}
	
	
	@AllArgsConstructor
	@Data
	public class uploadRequest {
		private String training_dir;
		private String api_key;
		private String algorithm_id;
	}
}
