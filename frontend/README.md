# Carepoint frontend

React + Vite frontend for the Hospital Appointment Booking API. It uses plain JavaScript, React Router, Axios, Tailwind CSS, and Recharts.

## Run the frontend

1. Start the Spring Boot API and MySQL using the root README instructions. The API must be reachable at `http://localhost:8080`.
2. Open a terminal in `frontend` and install dependencies:

   ```sh
   npm install
   ```

3. Copy `.env.example` to `.env`. Set `VITE_API_BASE_URL=http://localhost:8080` if the API uses the default address.
4. Start the development server:

   ```sh
   npm run dev
   ```

   Open the Vite URL (normally `http://localhost:5173`). Build a production bundle with `npm run build`.

In development, Vite proxies `/api` calls to `VITE_API_BASE_URL`, avoiding browser CORS preflights. The backend also permits the Vite origins `http://localhost:5173` and `http://127.0.0.1:5173` for direct local API requests.

The navbar saves your light/dark theme and English/Hindi/Telugu choice in local storage. Doctor availability refreshes every 10 seconds while visible; the appointment list refreshes every 15 seconds. Successful bookings show an in-app confirmation, queue the backend confirmation email, and can trigger a browser notification after the patient enables alerts.

For sample records and local email previews, start the repository's Docker Compose stack with `docker compose up --build`; demo data is enabled for that local stack and adds four fictional doctors with weekday slots. Confirmation emails are captured by Mailpit at `http://localhost:8025` and are not sent externally. Disable demo data with `DEMO_DATA_ENABLED=false` for other environments. Demo doctor accounts have generated random passwords and are not login accounts; use a registered patient to demonstrate booking.

## Pages and how to test them

1. **Sign in and register** (`/login`, `/register`): register a patient with a password between 12 and 72 characters, then sign in. Log in as configured bootstrap admin or an admin-created doctor to see role-specific navigation. The app keeps the access token in memory and stores only the rotating refresh token in local storage. This reduces the time an access token remains exposed to browser storage, while the refresh token survives reloads; XSS can still access local storage, so deploy with a strong Content Security Policy and avoid unsafe HTML.
2. **Find doctors** (`/doctors`): search by name, choose a specialization, set fee bounds, and use the page controls. Results come from the pageable `/api/doctors` endpoint.
3. **Doctor detail and booking** (`/doctors/:id`): open a doctor card, choose a future date, and book an open time while signed in as a patient. Try selecting one slot from two browser sessions; the losing booking should display the server's HTTP 409 message.
4. **My appointments** (`/appointments`): book a future visit first, then cancel it or choose a new date/time to reschedule. Actions are hidden inside the two-hour cutoff and after three reschedules. Use History to see the actor and timestamp for each status change.
5. **Patient profile** (`/profile`): edit the name, phone, or date of birth and save; reload to confirm the changes persisted.
6. **Doctor dashboard** (`/doctor`): log in as a doctor. Add one or more weekday working-hour rows, save them, then inspect that doctor's slots on the public doctor page. Add future leave dates and confirm those slots disappear. Edit the profile and mark booked visits completed or no-show.
7. **Admin dashboard** (`/admin`): log in as an admin. Create a specialization, create a doctor account with it, then review monthly appointments, cancellation rate, peak hours, and top specializations as charts.

Patient registration creates PATIENT accounts only. Doctor accounts are created from the admin dashboard. The UI shows backend error messages (including conflicts); frontend checks improve usability but backend validation and authorization remain authoritative.

## Source folders

- `src/api`: configured Axios client and endpoint functions
- `src/components`: shared page layout, navigation, status, loading, empty, and modal UI
- `src/context`: authentication and toast contexts
- `src/pages`: login, registration, doctor search/details, appointment, profile, and dashboards
- `src/routes`: role-protected route handling
- `src/utils`: JWT decoding and error-message helpers
