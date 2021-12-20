package org.mocaris.coins.wallet.imp

import org.mocaris.coins.wallet.coin.CoinType

/**
 * @Author mocaris
 * @Date 2019-11-04 15:12
 */
class ETCWalletImp : ETHWalletImp() {

    override fun coinType(): CoinType = CoinType.ETC
}
