/// support Coins
///

enum MainCoin { BTC, BCH, DASH, ETH, EOS, LTC, FIL, TRX }

extension CoinExt on MainCoin {
  String get coinName => toString().split('.')[1];
}
