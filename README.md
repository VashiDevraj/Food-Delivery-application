# Food-Delivery-application
A full-stack food delivery Android app built with Java and Firebase, supporting three distinct roles — Customer, Restaurant Admin, and Delivery Boy — with real-time order tracking, secure payments, and role-based dashboards.

Developed as a Major Project for T.Y.B.C.A. (6th Semester), Veer Narmad South Gujarat University, Surat — under Narmada College of Science & Commerce, Bharuch.

**Overview**

The Food Delivery App digitizes the complete food ordering and delivery workflow by connecting customers, restaurant managers, and delivery personnel on a single real-time platform — eliminating manual phone orders, miscommunication, and delivery tracking gaps.

Each of the three roles gets its own dedicated dashboard and feature set:

User (Customer): Browse restaurants and menus, manage cart, checkout, track orders live, rate deliveries.
Admin (Restaurant Owner): Manage menu items, process incoming orders, assign delivery partners, view analytics.
Delivery Boy: Accept/reject orders, update delivery status in real time, track earnings.
** Key Features**
** User**
Firebase Authentication (email/password + Google Sign-In)
Home screen with categories, offer banners, and restaurant browsing
Real-time search with autocomplete
Cart with live price updates and coupon support (including free-delivery coupons)
Checkout with saved addresses, delivery notes, and tip selection
Payments via Razorpay, UPI deep-link, or Cash on Delivery
Live order tracking (Pending → Confirmed → Out for Delivery → Delivered)
Order history and profile management
Post-delivery reviews with separate ratings for food quality, delivery speed, and packaging
** Admin (Restaurant Owner)**
Restaurant profile setup (hours, address, veg-only toggle, auto open/close)
Add, edit, delete food items with image upload
Real-time order management — confirm, reject, or assign to a delivery partner
Best Sellers analytics based on order history
Customer review dashboard
Coupon and discount management
** Delivery Boy**
Dedicated login/registration with vehicle details
Online/offline availability toggle
Live assigned-orders list with accept/reject
Step-by-step delivery status updates (Picked Up → Out for Delivery → Delivered)
Cash-on-delivery collection and UPI QR display
Earnings dashboard (today / monthly / all-time) with history
**Tech Stack**
Layer	Technology
Frontend	Java, XML (Android Studio)
Backend	Firebase Realtime Database
Authentication	Firebase Authentication
Storage	Firebase Storage
Notifications	Firebase Cloud Messaging (FCM)
Payments	Razorpay SDK
Documentation Tools	MS Word, draw.io
**Database Design**

Data is organized in Firebase Realtime Database under structured nodes:

users/          → uid, name, email, phone, accountType, address
foodItems/      → itemId, name, category, price, imageUrl, isAvailable
orders/         → orderId, customerId, items[], totalAmount, orderStatus, timestamps
cart/           → uid → itemId → quantity, price
addresses/      → uid → addressId → fullAddress, city, pincode
deliveryAssignments/ → orderId → deliveryBoyId, currentStatus
notifications/  → uid → notificationId → message, type, isRead

See the ER Diagram and DFD diagrams in this repo for the full data flow and entity relationships.

** Screenshots**
Login	Home	Cart
User/Admin/Delivery role-based login	Restaurant browsing & categories	Live cart with coupons
Checkout	Order Tracking	Admin Dashboard
Address, tip, payment	Real-time status updates	Orders, menu, analytics

(Add actual screenshot images to a /screenshots folder and update the table above with image links for the best presentation.)

 **Getting Started**
Clone the repository:
   git clone https://github.com/VashiDevraj/Food-Delivery-application.git
Open the project in Android Studio.
Connect your own Firebase project:
Create a project in Firebase Console
Enable Realtime Database, Authentication, and Storage
Download your own google-services.json and place it in app/
Sync Gradle and run the app on an emulator or physical device.

🔭 Future Scope
Multi-restaurant marketplace with a super-admin panel
Real-time GPS delivery tracking on a live map
AI-based food recommendations and demand forecasting
iOS version / cross-platform rewrite (Flutter or React Native)
Web-based admin dashboard
Scheduled orders and subscription meal plans

👨‍💻 Developer

Devraj Vashi BCA (NCF-NEP), Narmada College of Science & Commerce, Bharuch — Veer Narmad South Gujarat University, Surat Guided by: Mrs. Isha A. Shah

📄 License

This project was developed as an academic major project. Feel free to reference it for learning purposes.
