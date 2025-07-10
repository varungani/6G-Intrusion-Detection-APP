# 📡 6G Intrusion Detection App

A cutting-edge mobile application for real-time **Intrusion Detection in 6G Networks**, 
powered by the **Temporal Spatial Attention Network (TSA-Net)**. The app detects and 
classifies the 7 most common types of network attacks using 
the **CICIDS (Canadian Institute for Cybersecurity Intrusion Detection System)** dataset,achieving **over 97% accuracy**.

## 🧠 Key Features

- ⚙️ **TSA-Net Model**: A deep learning architecture that leverages Temporal-Spatial Attention to identify network intrusions with high precision.
- 📱 **Android Mobile App**: Built using **Android Studio**, the app is deployable on Android smartphones and can process data from network interfaces or simulated traffic.
- 🧪 **CICIDS Dataset**: Trained on the publicly available CICIDS dataset containing labeled attack traffic across different network protocols.
- 🎯 **7 Attack Classifications**:
  - DDoS
  - Brute Force
  - Port Scan
  - Botnet
  - Infiltration
  - Web Attacks
  - Normal (Benign)

## 📊 Performance

- ✅ **Accuracy**: 97.2%
- 🧠 **Model**: TSA-Net (Temporal Spatial Attention)(Quantized and Pruned)
- ⚡ **Inference**: Fast and lightweight, suitable for on-device predictions

## 📱 App Screenshots



## 🛠️ Tech Stack

| Component         | Technology        |
|------------------|-------------------|
| Model Architecture | TSA-Net (PyTorch/Keras) |
| Training Dataset | CICIDS (2017/2018) |
| Mobile Interface | Android Studio (Java) |
| Deployment | Android APK (Local & USB) |

## 🚀 Getting Started

### 1. Clone the Repository
```bash
git clone https://github.com/varungani/6G-Intrusion-Detection-APP.git
