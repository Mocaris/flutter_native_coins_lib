package org.mocaris.coins.wallet.imp

import com.google.common.base.Joiner
import org.bitcoinj.crypto.ChildNumber
import org.bitcoinj.crypto.DeterministicHierarchy
import org.bitcoinj.crypto.HDKeyDerivation
import org.bouncycastle.util.encoders.Hex
import org.json.JSONObject
import org.mocaris.coins.wallet.CoinWallet
import org.mocaris.coins.wallet.SignTransactionException
import org.mocaris.coins.wallet.coin.CoinType
import org.mocaris.coins.wallet.util.Utils
import org.web3j.crypto.*
import org.web3j.utils.Numeric


/**
 * @Author mocaris
 * @Date 2019-11-04 15:12
 */
open class BNBWalletImp : ETHWalletImp() {

    override fun coinType(): CoinType = CoinType.BNB

    override val chainId: Long = if (config.testNet) 97 else 56

}
