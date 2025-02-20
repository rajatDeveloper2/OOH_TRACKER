import 'package:lorryzone_tracker_app/base/base_service.dart';
import 'package:lorryzone_tracker_app/feature/auth/model/user_model.dart';
import 'package:lorryzone_tracker_app/network/api_const.dart';
import 'package:lorryzone_tracker_app/network/api_response.dart';
import 'package:lorryzone_tracker_app/network/network_manager.dart';
import 'package:lorryzone_tracker_app/network/network_request.dart';

class AuthService extends BaseService {
  //Send Otp to 2 Factor

  Future<ApiResponse> sendOTP2FactorService({
    required String mobileNumber,
    required String otp,
  }) async {
    NetworkRequest request = NetworkRequest(
      "${ApiConst.urlToVerifyP1}$mobileNumber/$otp${ApiConst.urlToVerifyP2}",
      RequestMethod.get,
      headers: getHeaders(),
    );

    final result = await NetworkManager.instance.perform(request);

    if (result.json != null) {
      result.data = result.json;
    }
    return result;
  }

  // verfiy the token

  Future<ApiResponse<UserModel?>> verifyUserTokenAndGetUserData({
    required String token,
  }) async {
    NetworkRequest request = NetworkRequest(
      ApiConst.verifyTheUserUrl,
      RequestMethod.post,
      headers: getAuthorizationHeaders(token: token),
    );

    final result = await NetworkManager.instance.perform<UserModel>(request);

    if (result.json != null) {
      var userMap = result.json['data'];

      UserModel user = UserModel.fromMap(userMap);
      result.data = user;
    }
    return result;
  }

  // genrate the Token

  Future<ApiResponse> genrateTokenService({
    required String mobileNumber,
    required String otp,
    required String uuid,
  }) async {
    var data = {"phone": mobileNumber, "otp": otp, "uuid": uuid};

    NetworkRequest request = NetworkRequest(
      ApiConst.genrateTokenUrl,
      RequestMethod.post,
      data: data,
      headers: getHeaders(),
    );

    final result = await NetworkManager.instance.perform(request);

    if (result.json != null) {
      result.data = result.json;
    }
    return result;
  }

  // registration

  Future<ApiResponse> registrationService({
    required String mobileNumber,
    required String otp,
    required String uuid,
  }) async {
    var data = {"phone": mobileNumber, "otp": otp, "uuid": uuid};

    NetworkRequest request = NetworkRequest(
      ApiConst.registerOtpUrl,
      RequestMethod.post,
      data: data,
      headers: getHeaders(),
    );

    final result = await NetworkManager.instance.perform(request);

    if (result.json != null) {
      result.data = result.json;
    }
    return result;
  }
}
