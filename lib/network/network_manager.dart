import 'package:dio/dio.dart';

import 'package:pretty_dio_logger/pretty_dio_logger.dart';

import 'api_response.dart';
import 'application_error.dart';
import 'network_request.dart';

class NetworkManager {
  static final NetworkManager instance = NetworkManager();

  final Dio _dio = Dio();

  NetworkManager() {
    initDio();
  }

  void initDio() {
    _dio.options.baseUrl = "";
    _dio.options.connectTimeout = const Duration(seconds: 20);
    _dio.options.receiveTimeout = const Duration(seconds: 20);

    _dio.interceptors.addAll(
      [
        InterceptorsWrapper(onRequest: (
          RequestOptions options,
          RequestInterceptorHandler handler,
        ) {
          // Authorization
          final bool isAuthorized = options.extra['isAuthorized'] as bool;
          if (isAuthorized) {
            // todo add authorization header
            // final String token = Get.find<SharedPreferencesService>().token;
            // options.headers['Authorization'] = 'Bearer $token';
          }
          // Language
          // todo add language interceptor
          /*final AppLocale locale = Get.find<LocalizationService>().currentLocale;
          if (locale.languageCode != null) {
            options.headers['Accept-Language'] = locale.languageCode;
          }*/
          return handler.next(options);
        }),
        PrettyDioLogger(
          requestHeader: true,
          requestBody: true,
          responseBody: true,
          responseHeader: true,
          compact: false,
        ),
      ],
    );
  }

  Future<ApiResponse<T>> perform<T>(NetworkRequest request) async {
    try {
      // final idToken = await FirebaseAuth.instance.currentUser?.getIdToken();
      // if(idToken != null){
      //   request.headers?['Authorization'] = idToken;
      // }

      final Response<dynamic> response = await _dio.request<dynamic>(
        request.url,
        data: request.data,
        queryParameters: request.queryParams,
        options: _getOptions(request),
      );
      return ApiResponse.success(response.data);
    } catch (e) {
      return ApiResponse.failed(
          getApplicationErrorFromDioError(e as DioException));
    }
  }

  Options _getOptions(NetworkRequest request) {
    return Options(
      headers: request.headers,
      method: request.method.name,
      extra: <String, dynamic>{
        'isAuthorized': request.isAuthorized
      }, // read this later in interceptor to send token if needed
    );
  }

  ApplicationError getApplicationErrorFromDioError(DioException dioError) {
    ErrorType errorType;
    String errorMsg = "Something went wrong! Please try again later.";
    dynamic extra;
    if (dioError.response?.data != null && dioError.response?.data is Map) {
      errorMsg = dioError.response?.data["error"].toString() ??
          dioError.response?.data;
      extra = dioError.response?.data["message"];
    }
    if (dioError.response?.statusCode == 401) {
      errorType = Unauthorized();
    } else if (dioError.response?.statusCode == 404) {
      errorType = ResourceNotFound();
    } else {
      errorType = UnExpected();
    }
    return ApplicationError(type: errorType, errorMsg: errorMsg, extra: extra);
  }
}
