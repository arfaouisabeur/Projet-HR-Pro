🏢 RHPro — Enterprise Human Resources Management Platform

A modern and intelligent Human Resources Management System developed using Java 21, JavaFX, and MySQL, designed to digitalize and automate the entire HR workflow inside companies.

RHPro combines traditional HR management with advanced technologies such as:

Artificial Intelligence
Machine Learning
Blockchain
OCR
Facial Recognition
Voice Synthesis
Real-Time Notifications
✨ Key Features
🔐 Authentication & Security
Secure email/password authentication
Google OAuth2 social login
Facial recognition login system
OTP password reset via email
Role-based access control
CAPTCHA protection
BCrypt password hashing
Session management system
💼 Recruitment & Hiring
Job offer management with geolocation
Interactive map integration
Online job applications
CV upload and parsing
AI-powered CV analysis
AI-generated cover letters
Candidate matching score
Blockchain certification for candidatures
QR code certificates
🌴 Leave Management
Employee leave requests
RH approval/rejection workflow
OCR medical certificate verification
AI-generated leave descriptions
SMS notifications
PDF leave reports
Voice synthesis reports
📁 Project & Task Management
Project management system
Intelligent AI task suggestions
Kanban task tracking
Employee performance scoring
Jitsi Meet video conferencing integration
Project PDF reports
Task progress statistics
💰 Payroll & Contracts
Employment contract management
Salary records
Bonus/primes management
Automated payroll tracking
PDF payslip generation
Employee payroll history
🎉 Events & Activities
Company event management
Event geolocation system
Participation tracking
AI participation prediction
Sentiment analysis on reviews
Rating system
Event image uploads
🛎 Service Requests
Employee service request system
AI service recommendations
Like/dislike reactions
RH workflow processing
SMS notifications
Voice reports generation
🤖 AI Assistant & Chatbot
Multilingual chatbot (FR / EN / AR)
Machine Learning intent detection
HR statistics assistant
Employee assistance system
📊 Dashboard & Analytics
Dynamic dashboards
Interactive charts
Employee statistics
Project analytics
Event participation metrics
Payroll summaries
👥 User Roles
Role	Description
ROLE_CANDIDAT	Apply for jobs, upload CVs, track applications
ROLE_EMPLOYE	Manage tasks, leaves, services, payroll, events
ROLE_RH	Full HR administration and system management

ROLE_RH inherits all permissions from employee and candidate roles.

🛠 Technology Stack
Backend
Technology	Version
Java	21
JavaFX	21
JDBC	Latest
MySQL	8.0
Maven	3.x
Hibernate ORM	6.x
Scene Builder	Latest
Frontend
JavaFX FXML
CSS Styling
Scene Builder
JavaFX Charts
Material Design UI
🤖 AI & Machine Learning
Service	Purpose
Groq API	LLM generation
HuggingFace	NLP processing
scikit-learn	Chatbot ML
OCR.space	OCR verification
ElevenLabs	Voice synthesis
🌐 External APIs
API	Usage
Twilio	SMS notifications
Geoapify	Geolocation
OpenWeatherMap	Weather data
Google OAuth2	Social authentication
Alchemy Ethereum	Blockchain certification
📂 Project Structure
RHPro/
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── controllers/
│   │   │   ├── entities/
│   │   │   ├── services/
│   │   │   ├── utils/
│   │   │   ├── dao/
│   │   │   ├── chatbot/
│   │   │   └── Main.java
│   │   │
│   │   ├── resources/
│   │   │   ├── fxml/
│   │   │   ├── css/
│   │   │   ├── images/
│   │   │   └── sounds/
│
├── chatbot_ml/                 # Python chatbot microservice
├── facial_recognition/         # Facial recognition microservice
├── database/
│   └── rhpro.sql
│
├── pom.xml
└── README.md
🧩 Main Entities
Entity	Description
User	Base authenticated user
Employe	Employee profile
RH	HR manager profile
Candidat	Candidate profile
Projet	Project management
Tache	Task management
CongeTt	Leave requests
OffreEmploi	Job offers
Candidature	Job applications
Salaire	Payroll records
Prime	Employee bonuses
Evenement	Company events
DemandeService	Service requests
🤖 AI & Smart Services
Integrated AI Features
AI task generation
AI leave description generation
AI CV analysis
AI participation prediction
AI service recommendations
AI chatbot assistant
🔗 Blockchain Integration

RHPro uses Ethereum Sepolia blockchain through Alchemy API for:

Candidature certification
Integrity verification
Document authenticity
📱 Notifications System
Email notifications
SMS alerts
OTP verification
Real-time reminders
🔍 OCR & Facial Recognition
OCR Verification

Medical certificates are automatically analyzed using OCR technology.

Facial Recognition

Employees can authenticate using facial recognition through a Python Flask microservice connected to JavaFX.

⚙️ Installation
1️⃣ Clone Repository
git clone <repository-url>

cd RHPro
2️⃣ Configure Database

Create MySQL database:

CREATE DATABASE rhpro;

Import database:

mysql -u root -p rhpro < rhpro.sql
3️⃣ Configure Environment

Edit database configuration inside:

src/main/java/utils/MyConnection.java

Example:

private final String url = "jdbc:mysql://localhost:3306/rhpro";
private final String user = "root";
private final String password = "";
4️⃣ Install Dependencies
mvn clean install
5️⃣ Run Application
mvn javafx:run
🐍 Python Microservices
Chatbot Service
cd chatbot_ml

pip install flask flask-cors scikit-learn

python api_chatbot.py

Runs on:

http://127.0.0.1:5001
Facial Recognition Service
pip install flask flask-cors face_recognition opencv-python

python face_recognition_api.py

Runs on:

http://127.0.0.1:5002
🧪 Testing
Maven Tests
mvn test
📌 Useful Commands
# Clean project
mvn clean

# Compile project
mvn compile

# Run application
mvn javafx:run

# Package application
mvn package
🔒 Security Best Practices
Never expose API keys publicly
Use BCrypt password hashing
Store credentials securely
Use HTTPS APIs only
Validate all user inputs
Protect sensitive endpoints
🚀 Future Improvements
Docker deployment
Kubernetes orchestration
Mobile application
Real-time notifications with WebSockets
AI analytics dashboard
Multi-company SaaS architecture
👨‍💻 Authors

RHPro was developed as an advanced academic and enterprise HR management platform project using modern Java architecture and intelligent services.

📄 License

Educational and academic use only.
