package com.vernu.sms.services;

import com.vernu.sms.dtos.DeviceHeartbeatRequestDTO;
import com.vernu.sms.dtos.DeviceStatusUpdateRequestDTO;
import com.vernu.sms.dtos.EnrollDeviceRequestDTO;
import com.vernu.sms.dtos.EnrollDeviceResponseDTO;
import com.vernu.sms.dtos.InboundSmsRequestDTO;
import com.vernu.sms.dtos.PendingMessagesResponseDTO;
import com.vernu.sms.dtos.SMSForwardResponseDTO;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface GatewayApiService {
    @POST("api/sms-gateway/devices/enroll")
    Call<EnrollDeviceResponseDTO> enrollDevice(@Body EnrollDeviceRequestDTO body);

    @POST("api/sms-gateway/devices/heartbeat")
    Call<EnrollDeviceResponseDTO> syncDevice(@Header("Authorization") String authorization, @Body DeviceHeartbeatRequestDTO body);

    @POST("api/sms-gateway/devices/inbound")
    Call<SMSForwardResponseDTO> sendReceivedSMS(@Header("Authorization") String authorization, @Body InboundSmsRequestDTO body);

    @POST("api/sms-gateway/devices/status")
    Call<SMSForwardResponseDTO> updateMessageStatus(@Header("Authorization") String authorization, @Body DeviceStatusUpdateRequestDTO body);

    @GET("api/sms-gateway/devices/pending")
    Call<PendingMessagesResponseDTO> fetchPendingMessages(@Header("Authorization") String authorization);
}
