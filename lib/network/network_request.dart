enum RequestMethod { get, post, put, delete, patch }

enum RequestDataType { json, formData }

extension RequestMethodExtension on RequestMethod {
  String get name {
    switch (this) {
      case RequestMethod.get:
        return 'get';
      case RequestMethod.post:
        return 'post';
      case RequestMethod.put:
        return 'put';
      case RequestMethod.delete:
        return 'delete';
      case RequestMethod.patch:
        return 'patch';
    }
  }
}

class NetworkRequest {
  NetworkRequest(this.url, this.method,
      {this.data,
      this.dataType = RequestDataType.json,
      this.queryParams,
      this.headers,
      this.isAuthorized = false});

  final String url;
  final RequestMethod method;
  dynamic data;
  RequestDataType dataType;
  Map<String, dynamic>? queryParams;
  Map<String, String>? headers;
  bool isAuthorized;
}
