#include <iostream>
#include <thread>
#include <chrono>

int main() {
    std::cout << "Program started. Waiting for 3 seconds..." << std::endl;

    // Sleep for 3000 milliseconds (3 seconds)
    std::this_thread::sleep_for(std::chrono::milliseconds(3000));

    std::cout << "Time's up! Crashing now..." << std::endl;

    // Trigger a crash (Segmentation Fault) by dereferencing a null pointer
    int* ptr = nullptr;
    *ptr = 42;

    // This line will never be reached
    return 0;
}
