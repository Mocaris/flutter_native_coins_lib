import 'dart:async';

import 'package:flutter/services.dart';
import 'package:native_coins_lib/support_coins.dart';
export 'support_coins.dart';

class NativeCoinsLib {
  static const MethodChannel _channel = MethodChannel('native_coins_lib');

  ///初始化钱包配置
  static Future<bool?> initWalletConfig({required bool testNet}) {
    return _channel.invokeMethod<bool>("initConfig", {"testNet": testNet});
  }

  ///获取所有 单词
  static Future<List<String>?> getAllMnemonicWords() {
    return _channel.invokeMethod<List<String>>("getAllMnemonicWords");
  }

  ///创建助记词
  static Future<List<String>?> generateMnemonicWords() {
    return _channel.invokeMethod<List<String>>("generateMnemonic");
  }

  ///检查助记词
  static Future<bool?> checkMnemonicWords(List<String> words) {
    return _channel.invokeMethod<bool>("checkMnemonicWords", {"words": words});
  }

  ///地址
  static Future<String?> getAddress({required List<String> words, required MainCoin coin, String passPhrase = ""}) {
    return _channel.invokeMethod<String>("getAddress", {"words": words, "coinName": coin.coinName, passPhrase: passPhrase});
  }

  ///验证地址
  static Future<bool?> checkAddress(String address, MainCoin coin) {
    return _channel.invokeMethod<bool>("checkAddress", {"address": address, "coinName": coin.coinName});
  }

  ///签名
  static Future<String?> signTransaction(
      {required String words,
      required MainCoin coin,
      required String inputTransaction,
      required String addressTo,
      String passPhrase = ""}) {
    return _channel.invokeMethod<String>("signTransaction", {
      "words": words,
      "coinName": coin.coinName,
      "inputTransaction": inputTransaction,
      "addressTo": addressTo,
      "passPhrase": passPhrase
    });
  }
}
