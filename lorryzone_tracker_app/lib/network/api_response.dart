import 'application_error.dart';

class ApiResponse<T> {
  T? data;
  dynamic json;
  late Status status;
  ApplicationError? error;

  ApiResponse.success(this.json) {
    status = Status.ok;
  }

  ApiResponse.failed(ApplicationError this.error) {
    status = Status.failed;
  }

  handleResponse({Function? onSuccess, Function? onFailed}) async {
    if (status == Status.ok) {
      await onSuccess?.call();
    } else if (status == Status.failed) {
      await onFailed?.call();
    }
    return Future.value();
  }
}

enum Status { ok, failed }
