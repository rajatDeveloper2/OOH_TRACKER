class ApiConst {
  static const String baseUrl = "https://client.oohtracker.com/api";
  static const String registerOtpUrl = "$baseUrl/registerOTP";
  static const String urlToVerifyP1 =
      "https://2factor.in/API/V1/cfd6dbca-321e-11e6-b006-00163ef91450/SMS/"; // +phone / otp / p2
  static const String urlToVerifyP2 = "/LZOTP";
  static const String verifyTheUserUrl = "$baseUrl/verify";
  static const String genrateTokenUrl = "$baseUrl/token";
}
