factory/
├── build.gradle
├── settings.gradle
└── src/
    ├── main/
    │   ├── java/ru/nsu/ccfit/Tanya/
    │   │   ├── threadpool/
    │   │   │   ├── Task.java
    │   │   │   ├── WorkerThread.java
    │   │   │   └── ThreadPool.java
    │   │   └── factory/
    │   │       ├── config/FactoryConfig.java
    │   │       ├── model/
    │   │       │   ├── Part.java
    │   │       │   ├── Body.java
    │   │       │   ├── Engine.java
    │   │       │   ├── Accessory.java
    │   │       │   ├── Car.java
    │   │       │   └── Storage.java
    │   │       ├── threads/
    │   │       │   ├── AssemblyTask.java
    │   │       │   ├── StockController.java
    │   │       │   ├── BodySupplier.java
    │   │       │   ├── EngineSupplier.java
    │   │       │   ├── AccessorySupplier.java
    │   │       │   └── Dealer.java
    │   │       ├── gui/FactoryGUI.java
    │   │       ├── SaleLogger.java
    │   │       └── Main.java
    │   └── resources/
    │       ├── factory.properties
    │       └── log4j.properties
    └── test/java/ru/nsu/ccfit/Tanya/
        ├── threadpool/ThreadPoolTest.java
        ├── factory/StorageTest.java
        └── factory/FactoryLogicTest.java
