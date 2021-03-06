package au.gov.dto.dibp.appointments.organisation;

import au.gov.dto.dibp.appointments.qflowintegration.ApiCallsSenderService;
import au.gov.dto.dibp.appointments.util.ResponseWrapper;
import au.gov.dto.dibp.appointments.util.TemplateLoader;
import com.samskivert.mustache.Template;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class UnitDetailsService {

    private final ServiceDetailsService serviceDetailsService;
    private final ApiCallsSenderService senderService;
    private final TimeZoneDictionary timeZoneDictionary;

    private final String serviceAddress;

    private final Template getUnitTemplate;
    private final Template getUnitLocalTimeTemplate;

    @Autowired
    public UnitDetailsService(ServiceDetailsService serviceDetailsService,
                              ApiCallsSenderService senderService,
                              TimeZoneDictionary timeZoneDictionary,
                              TemplateLoader templateLoader,
                              @Value("${SERVICE.ADDRESS.UNIT}") String serviceAddress){
        this.serviceDetailsService = serviceDetailsService;
        this.senderService = senderService;
        this.timeZoneDictionary = timeZoneDictionary;
        this.serviceAddress = serviceAddress;

        getUnitTemplate = templateLoader.loadRequestTemplate(GetUnit.REQUEST_TEMPLATE_PATH);
        getUnitLocalTimeTemplate = templateLoader.loadRequestTemplate(GetUnitLocalTime.REQUEST_TEMPLATE_PATH);
    }

    public String getUnitAddress(String unitId){
        Map<String, String> data = new HashMap<>();
        data.put("unitId", unitId);

        ResponseWrapper response = senderService.sendRequest(getUnitTemplate, data, serviceAddress);
        return response.getStringAttribute(GetUnit.UNIT_ADDRESS);
    }

    public LocalDateTime getUnitCurrentLocalTime(String unitId){
        Map<String, String> data = new HashMap<>();
        data.put("unitId", unitId);

        ResponseWrapper response = senderService.sendRequest(getUnitLocalTimeTemplate, data, serviceAddress);
        return LocalDateTime.parse(response.getStringAttribute(GetUnitLocalTime.LOCAL_TIME));
    }

    public UnitDetails getUnitDetails(String unitId){
        Map<String, String> data = new HashMap<>();
        data.put("unitId", unitId);

        ResponseWrapper response = senderService.sendRequest(getUnitTemplate, data, serviceAddress);
        String id = response.getStringAttribute(GetUnit.UNIT_ID);
        String address = response.getStringAttribute(GetUnit.UNIT_ADDRESS);
        String timeZoneId = response.getStringAttribute(GetUnit.UNIT_TIMEZONE_ID);

        return new UnitDetails(id, address, timeZoneDictionary.getTimeZoneIANA(timeZoneId));
    }

    public String getUnitAddressByServiceId(String serviceId){
        String unitId = serviceDetailsService.getUnitIdForService(serviceId);
        return getUnitAddress(unitId);
    }

    public LocalDateTime getUnitCurrentLocalTimeByServiceId(String serviceId){
        String unitId = serviceDetailsService.getUnitIdForService(serviceId);
        return getUnitCurrentLocalTime(unitId);
    }

    public UnitDetails getUnitDetailsByServiceId(String serviceId){
        String unitId = serviceDetailsService.getUnitIdForService(serviceId);
        return getUnitDetails(unitId);
    }

    private class GetUnit {
        static final String REQUEST_TEMPLATE_PATH = "GetUnit.mustache";
        static final String UNIT_ID = "//GetResponse/GetResult/Id";
        static final String UNIT_TIMEZONE_ID = "//GetResponse/GetResult/TimeZoneId";
        static final String UNIT_ADDRESS = "//GetResponse/GetResult/Address";
    }

    private class GetUnitLocalTime {
        static final String REQUEST_TEMPLATE_PATH = "GetUnitLocalTime.mustache";
        static final String LOCAL_TIME = "//GetLocalTimeResponse/GetLocalTimeResult";
    }
}
