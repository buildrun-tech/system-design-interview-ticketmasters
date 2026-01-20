# Product Overview

TicketMaster is a sample event-ticketing backend system designed as a system design interview project. It demonstrates a microservice architecture for managing events, bookings, users, and authentication.

## Core Features

- **Event Management**: Create and manage events with configurable seating
- **Booking System**: Reserve seats with pending/confirmed/rejected states and automatic expiration
- **User Management**: User registration, authentication, and role-based access
- **Admin Functions**: Administrative operations for system management
- **Asynchronous Processing**: Background SQS consumers for booking state management
- **JWT Authentication**: Secure token-based authentication with role-based permissions

## Business Domain

The system models a ticket booking platform where:
- Events have configurable numbers of seats
- Users can reserve seats (creating pending bookings)
- Bookings must be confirmed within a time window or they expire
- Admin users can manage the system and approve/reject bookings
- Background processes handle booking expiration automatically

## Target Use Case

This is a demonstration/interview project showcasing:
- Microservice patterns
- Event-driven architecture with SQS
- JWT-based security
- Database design with JPA/Hibernate
- RESTful API design