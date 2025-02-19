class ApplicationError implements Exception{
  ApplicationError({required this.type, required this.errorMsg, this.errorMsgKey, this.extra});

  final ErrorType type;
  final String errorMsg;
  final String? errorMsgKey;
  final dynamic extra;
}

class ErrorType{}

class NetworkError extends ErrorType{
  final int code;

  NetworkError(this.code);
}

class Unauthorized extends NetworkError{
  Unauthorized() : super(401);
}

class ResourceNotFound extends NetworkError{
  ResourceNotFound() : super(404);
}

class UnExpected extends NetworkError{
  UnExpected() : super(-1);
}