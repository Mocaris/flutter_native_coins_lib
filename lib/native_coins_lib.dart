import 'dart:async';

import 'package:flutter/services.dart';
import 'package:native_coins_lib/support_coins.dart';

export 'encrypt/aes.dart';
export 'encrypt/base58.dart';
export 'mnemonic_util.dart';
export 'support_coins.dart';

class NativeCoinsLib {
  static const MethodChannel _channel = MethodChannel('native_coins_lib');

  ///初始化钱包配置
  Future<bool?> initWalletConfig({required bool testNet}) {
    return _channel.invokeMethod<bool>("initWalletConfig", {"testNet": testNet});
  }

  ///获取所有 单词
  Future<List<String>?> getAllMnemonicWords() async {
    var res = await _channel.invokeMethod<List>("getAllMnemonicWords");
    return res?.map((e) => e.toString()).toList();
  }

  ///创建助记词
  Future<List<String>?> generateMnemonicWords() async {
    var res = await _channel.invokeMethod<List>("generateMnemonic");
    return res?.map((e) => e.toString()).toList();
  }

  ///检查助记词
  Future<bool?> checkMnemonicWords(List<String> words) {
    return _channel.invokeMethod<bool>("checkMnemonicWords", {"words": words});
  }

  ///地址
  Future<String?> getAddress({required List<String> words, required MainCoin coin, String passPhrase = ""}) {
    return _channel.invokeMethod<String>("getAddress", {"words": words, "coinName": coin.coinName, "passPhrase": passPhrase});
  }

  ///验证地址
  Future<bool?> checkAddress(String address, MainCoin coin) {
    return _channel.invokeMethod<bool>("checkAddress", {"address": address, "coinName": coin.coinName});
  }

  ///私钥
  Future<String?> getPrivateKeyHex({required List<String> words, required MainCoin coin, String passPhrase = ""}) {
    return _channel.invokeMethod<String>("getPrivateKeyHex", {"words": words, "coinName": coin.coinName, "passPhrase": passPhrase});
  }

  ///公钥
  Future<String?> getPublicKeyHex({required List<String> words, required MainCoin coin, String passPhrase = ""}) {
    return _channel.invokeMethod<String>("getPublicKeyHex", {"words": words, "coinName": coin.coinName, "passPhrase": passPhrase});
  }

  ///签名
  Future<String?> signTransaction(
      {required List<String> words,
      required MainCoin coin,
      required String inputTransaction,
      String passPhrase = ""}) {
    return _channel.invokeMethod<String>("signTransaction", {
      "words": words,
      "coinName": coin.coinName,
      "inputTransaction": inputTransaction,
      "passPhrase": passPhrase
    });
  }

  ///eth Hash2Rvs签名
  Future<String?> ethSignHash2Rvs({required List<String> words, required String signHash, String passPhrase = ""}) {
    return _channel.invokeMethod<String>("ethSignHash2Rvs", {"words": words, "signHash": signHash, "passPhrase": passPhrase});
  }
}
