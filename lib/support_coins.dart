/// support Coins
///

enum MainCoin { BTC, BCH, DASH, ETH, EOS, LTC, FIL, TRX }

class CoinType {
  String coinName;
  MainCoin mainCoin;

  CoinType({required this.coinName, required this.mainCoin});

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is CoinType && runtimeType == other.runtimeType && coinName == other.coinName && mainCoin == other.mainCoin;

  @override
  int get hashCode => coinName.hashCode ^ mainCoin.hashCode;

  //main
  static final supportMain = MainCoin.values.map((e) => CoinType(coinName: e.coinName, mainCoin: e)).toList();

  static final supportToken = [TUSDT, EUSDT];

  static final supportAll = List.from(supportMain)..addAll(supportToken);

  static final EUSDT = CoinType(coinName: "EUSDT", mainCoin: MainCoin.ETH);

  static final TUSDT = CoinType(coinName: "TUSDT", mainCoin: MainCoin.TRX);
}

extension CoinExt on MainCoin {
  String get coinName => toString().split('.')[1];
}

class CoinsUtil {
  static CoinType? coinNameOf(String name) {
    var coinName = name.toUpperCase();
    for (var coin in CoinType.supportAll) {
      if (coinName == coin.coinName) {
        return coin;
      }
    }
    return null;
  }
}
