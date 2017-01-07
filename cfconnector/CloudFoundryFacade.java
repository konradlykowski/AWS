package default;

import lombok.extern.log4j.Log4j;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.operations.services.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
@Log4j
public class CloudFoundryFacade {


    @Autowired
    DefaultCloudFoundryOperations cloudFoundryOperations;

    @Value("${cf.serviceInstanceName}")
    String serviceInstanceName;
    @Value("${cf.serviceName}")
    String serviceName;
    @Value("${cf.planName}")
    String planName;
    @Value("${cf.serviceKeyName}")
    String serviceKeyName;


    public Map<String, Object> getServiceKeyCredentials() {
        createServiceIfNotExistsBlocking(serviceInstanceName, serviceName, planName);
        createServiceKeyIfNotExistsBlocking(serviceKeyName, serviceInstanceName);
        return getServiceKeyBlocking(serviceKeyName, serviceInstanceName).getCredentials();
    }

    private ServiceKey getServiceKeyBlocking(String serviceKeyName, String serviceInstanceName) {
        return this.cloudFoundryOperations.services().getServiceKey(GetServiceKeyRequest.builder()
                .serviceKeyName(serviceKeyName).serviceInstanceName(serviceInstanceName).build())
                .doOnSubscribe(s -> log.info("Getting a service key " + serviceKeyName))
                .block();
    }

    private void createServiceKeyIfNotExistsBlocking(String keyName, String serviceInstanceName) {
        this.cloudFoundryOperations.services()
                .listServiceKeys(ListServiceKeysRequest.builder().serviceInstanceName(serviceInstanceName).build())
                .filter(serviceKey -> keyName.equals(serviceKey.getName()))
                .singleOrEmpty()
                .switchIfEmpty(this.cloudFoundryOperations.services()
                        .createServiceKey(CreateServiceKeyRequest.builder().serviceInstanceName(serviceInstanceName)
                                .serviceKeyName(keyName)
                                .build())
                        .cast(ServiceKey.class)
                        .doOnSubscribe(s -> log.info("Creating service key " + keyName)))
                .block();
    }

    private void createServiceIfNotExistsBlocking(String serviceInstanceStr, String serviceName, String planName) {
        this.cloudFoundryOperations.services()
                .listInstances()
                .filter(serviceInstance -> serviceInstanceStr.equals(serviceInstance.getName()))
                .singleOrEmpty()
                .switchIfEmpty(createNewServiceInstance(serviceInstanceStr, serviceName, planName))
                .then().block();
    }

    private Mono<ServiceInstanceSummary> createNewServiceInstance(String serviceInstanceStr, String serviceName, String planName) {
        Mono<ServiceInstanceSummary> mono = null;
        mono = this.cloudFoundryOperations.services()
                .createInstance(CreateServiceInstanceRequest.builder()
                        .planName(planName)
                        .serviceName(serviceName)
                        .serviceInstanceName(serviceInstanceStr)
                        .build())
                .cast(ServiceInstanceSummary.class)
                .doOnSubscribe(s -> log.info("Creating service " + serviceInstanceStr))
                .doOnError(e -> {
                    log.error("Failed to create a service " + e);
                });
        return mono;
    }

}
