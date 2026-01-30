import Foundation

public protocol NetworkMonitoring {
  var isOnline: Bool { get }
}

public struct StaticNetworkMonitor: NetworkMonitoring {
  public var isOnline: Bool

  public init(isOnline: Bool) {
    self.isOnline = isOnline
  }
}
