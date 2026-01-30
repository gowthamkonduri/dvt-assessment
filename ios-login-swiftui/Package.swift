// swift-tools-version: 5.9
import PackageDescription

let package = Package(
  name: "LoginFeature",
  platforms: [
    .iOS(.v16),
    .macOS(.v13)
  ],
  products: [
    .library(name: "LoginFeature", targets: ["LoginFeature"])
  ],
  targets: [
    .target(
      name: "LoginFeature",
      path: "Sources/LoginFeature"
    ),
    .testTarget(
      name: "LoginFeatureTests",
      dependencies: ["LoginFeature"],
      path: "Tests/LoginFeatureTests"
    )
  ]
)
