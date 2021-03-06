package au.gov.dto.dibp.appointments.availabletimes;

import au.gov.dto.dibp.appointments.client.Client;
import au.gov.dto.dibp.appointments.qflowintegration.ApiCallsSenderService;
import au.gov.dto.dibp.appointments.util.ResponseWrapper;
import au.gov.dto.dibp.appointments.util.TemplateLoader;
import com.samskivert.mustache.Template;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AvailableTimesService {


    private final ApiCallsSenderService senderService;
    private final String serviceAddress;

    private final Template getDynamicSuggestedSlots2Template;

    @Autowired
    public AvailableTimesService(ApiCallsSenderService senderService,
                                 TemplateLoader templateLoader,
                                 @Value("${SERVICE.ADDRESS.CALENDAR}") String serviceAddress) {
        this.senderService = senderService;
        this.serviceAddress = serviceAddress;

        getDynamicSuggestedSlots2Template = templateLoader.loadRequestTemplate(GetDynamicSuggestedSlots2.REQUEST_TEMPLATE_PATH);
    }

    public List<String> getAvailableTimes(Client client, String calendarId) {
        Map<String, String> data = new HashMap<>();
        data.put("calendarId", calendarId);
        data.put("appointmentTypeId", client.getAppointmentTypeId());

        ResponseWrapper response = senderService.sendRequest(getDynamicSuggestedSlots2Template, data, serviceAddress);
        return parseAvailableTimesResponse(response);
    }

    public Map<String, List<String>> getAvailableTimesWithCalendarDate(Client client, String calendarId) {
        Map<String, String> data = new HashMap<>();
        data.put("calendarId", calendarId);
        data.put("appointmentTypeId", client.getAppointmentTypeId());

        ResponseWrapper response = senderService.sendRequest(getDynamicSuggestedSlots2Template, data, serviceAddress);
        final List<String> availableTimes = parseAvailableTimesResponse(response);
        String date = response.getStringAttribute(GetDynamicSuggestedSlots2.CALENDAR_DATE);
        Map<String, List<String>> result = new HashMap<>();
        result.put(date, availableTimes);
        return  result;
    }

    private List<String> parseAvailableTimesResponse(ResponseWrapper response) {
        NodeList nodes = response.getNodeListAttribute(GetDynamicSuggestedSlots2.START_TIME);
        List<String> availableTimes = new ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            availableTimes.add(convertMinutesToHour(node.getTextContent()));
        }
        return availableTimes;
    }

    private String convertMinutesToHour(String minutesString){
        int timeInMinutes = Integer.parseInt(minutesString);
        int minutes = timeInMinutes % 60;
        int hours = (timeInMinutes-minutes) / 60;
        return String.format("%1$02d:%2$02d", hours, minutes);
    }

    private class GetDynamicSuggestedSlots2{
        private static final String REQUEST_TEMPLATE_PATH = "GetDynamicSuggestedSlots2.mustache";
        private static final String START_TIME = "//GetDynamicSuggestedSlots2Response/GetDynamicSuggestedSlots2Result/SuggestedSlots/DynamicCalendarSuggestedSlotItem/StartTime";
        private static final String CALENDAR_DATE = "//GetDynamicSuggestedSlots2Response/GetDynamicSuggestedSlots2Result/AvailableSequences/CalendarDate";
    }
}
