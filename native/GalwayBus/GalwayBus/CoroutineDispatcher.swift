import Foundation
import UIKit
import SharedCode


public class CoroutineDispatcher: Kotlinx_coroutines_core_nativeCoroutineScope {
    override public func dispatch(context: KotlinCoroutineContext, block: Kotlinx_coroutines_core_nativeRunnable) {
        DispatchQueue.main.async {
            block.run()
        }
    }
}
