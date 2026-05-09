# Memory Allocation Simulator

A JavaFX-based desktop application that simulates memory management using common dynamic partitioning strategies. This project provides a visual representation of how memory is allocated and deallocated for processes with multiple segments.

## 🚀 Features

- **Allocation Algorithms**: 
  - **First Fit**: Allocates the first hole that is big enough.
  - **Best Fit**: Allocates the smallest hole that is big enough to minimize wasted space.
- **Dynamic Partitioning**: 
  - Define initial memory holes (free partitions) at specific addresses.
  - Define processes with multiple named segments (e.g., Code, Data, Stack).
- **All-or-Nothing Policy**: A process is only allocated if all its segments can fit in memory simultaneously.
- **Deallocation & Merging**: 
  - Free memory by deallocating entire processes.
  - Automatically merges adjacent free holes to prevent fragmentation.
- **Visual Memory Map**: Real-time visualization of the memory layout, showing allocated segments and free holes.
- **Detailed Logs**: Step-by-step logging of allocation attempts and memory state changes.

## 🛠️ Technologies Used

- **Java 17**
- **JavaFX 21** (for the Graphical User Interface)
- **Maven** (for dependency management and build automation)

## 📦 Project Structure

```text
src/main/java
├── controller    # UI logic and event handlers
├── model         # Core memory management logic (MemoryManager, Partition, Process, Segment)
└── Launcher.java # Application entry point
src/main/resources
├── fxml          # UI layout files
└── css           # Styling for the application
```

## 🚀 How to Run

### Prerequisites
- JDK 17 or higher
- Maven installed

### Steps to Run
1. Clone the repository:
   ```bash
   git clone https://github.com/KirllosAtef/MemoryAllocation_Spring2026.git
   cd MemoryAllocation_Spring2026
   ```
2. Run using Maven:
   ```bash
   mvn javafx:run
   ```
3. (Optional) Build a runnable JAR:
   ```bash
   mvn clean package
   ```
   The executable JAR will be located in the `target/` directory with the `-fat` suffix.

## 📸 Screenshots
*(Add your screenshots here to show off the beautiful UI!)*

---
*Developed as part of the Operating Systems course - Spring 2026.*
