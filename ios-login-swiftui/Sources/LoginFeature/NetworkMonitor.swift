import Foundation
import Network

public protocol NetworkMonitoring: Sendable {
  var isOnline: Bool { get }
}

/// Static network monitor for testing.
public struct StaticNetworkMonitor: NetworkMonitoring {
  public let isOnline: Bool

  public init(isOnline: Bool) {
    self.isOnline = isOnline
  }
}

/// Real network monitor using NWPathMonitor.
@MainActor
public final class NetworkMonitor: ObservableObject {
  @Published public private(set) var isOnline: Bool = true

  private let monitor = NWPathMonitor()
  private let queue = DispatchQueue(label: "NetworkMonitor")

  public init() {
    monitor.pathUpdateHandler = { [weak self] path in
      Task { @MainActor in
        self?.isOnline = path.status == .satisfied
      }
    }
    monitor.start(queue: queue)
  }

  deinit {
    monitor.cancel()
  }
}

/// Wrapper to make NetworkMonitor conform to NetworkMonitoring.
public struct NetworkMonitorWrapper: NetworkMonitoring {
  private let monitor: NetworkMonitor

  @MainActor
  public init(monitor: NetworkMonitor) {
    self.monitor = monitor
  }

  public var isOnline: Bool {
    MainActor.assumeIsolated { monitor.isOnline }
  }
}
