// import 'dart:developer';

import 'dart:developer';

class BaseService {
  Map<String, String> getHeaders() {
    return {"Accept": "application/json", "Content-Type": "application/json"};
  }

  Map<String, String> getHeadersWithMultiMediaType() {
    return {"Content-Type": "multipart/form-data"};
  }

  Map<String, String> getAuthorizationHeaders({required String token}) {
    return {
      "Accept": "application/json",
      "Content-Type": "application/json",
      "Authorization": "Bearer $token",
    };
  }

  Map<String, String> getMultiMediaTypeNormal() {
    // log("Token of user ${TokenClass.token}");
    return {"Content-Type": "multipart/form-data"};
  }

  Map<String, String> getMultiMediaType({required String token}) {
    // log("Token of user ${TokenClass.token}");
    return {
      "Content-Type": "multipart/form-data",
      "Authorization": "Bearer $token",
    };
  }
}
