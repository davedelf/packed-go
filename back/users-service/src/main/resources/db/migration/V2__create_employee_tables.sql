-- Migration script for Employee system
-- Created: 2025-11-12
-- Description: Creates employees and employee_events tables

-- Create employees table
CREATE TABLE IF NOT EXISTS employees (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    username VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    document BIGINT NOT NULL UNIQUE,
    admin_id BIGINT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_employee_admin FOREIGN KEY (admin_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create employee_events junction table
CREATE TABLE IF NOT EXISTS employee_events (
    employee_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL,
    PRIMARY KEY (employee_id, event_id),
    CONSTRAINT fk_employee_events_employee FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE
);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_employees_email ON employees(email);
CREATE INDEX IF NOT EXISTS idx_employees_document ON employees(document);
CREATE INDEX IF NOT EXISTS idx_employees_admin_id ON employees(admin_id);
CREATE INDEX IF NOT EXISTS idx_employees_is_active ON employees(is_active);
CREATE INDEX IF NOT EXISTS idx_employee_events_employee_id ON employee_events(employee_id);
CREATE INDEX IF NOT EXISTS idx_employee_events_event_id ON employee_events(event_id);

-- Add comments for documentation
COMMENT ON TABLE employees IS 'Stores employee information created by event admins';
COMMENT ON TABLE employee_events IS 'Junction table linking employees to events they can work on';
COMMENT ON COLUMN employees.admin_id IS 'Foreign key to users table - the admin who created this employee';
COMMENT ON COLUMN employees.is_active IS 'Flag to enable/disable employee access without deleting the record';
