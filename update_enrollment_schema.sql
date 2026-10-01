-- =============================================================================
-- SCRIPT COMPLETO DE INICIALIZACIÓN DE BASE DE DATOS EN SUPABASE (ALERTA UNI)
-- Incluye Tablas, Vistas, RPC y Datos Iniciales. Copiar y pegar en SQL Editor.
-- =============================================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. TABLA: STUDENT
CREATE TABLE IF NOT EXISTS public.student (
    student_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    firstname TEXT NOT NULL,
    surname1 TEXT NOT NULL,
    surname2 TEXT,
    email TEXT,
    codigo TEXT,
    token_msg TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 2. TABLA: PROFESSOR
CREATE TABLE IF NOT EXISTS public.professor (
    professor_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    firstname TEXT NOT NULL,
    surname1 TEXT NOT NULL,
    surname2 TEXT,
    email TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 3. TABLA: COURSE
CREATE TABLE IF NOT EXISTS public.course (
    course_id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    code TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 4. TABLA: COURSE_CATALOG
CREATE TABLE IF NOT EXISTS public.course_catalog (
    course_catalog_id TEXT PRIMARY KEY,
    professor_id UUID REFERENCES public.professor(professor_id) ON DELETE SET NULL,
    course_id TEXT REFERENCES public.course(course_id) ON DELETE CASCADE,
    group_type TEXT DEFAULT 'GRUPO_A',
    course_type TEXT DEFAULT 'OBLIGATORIO',
    semester TEXT NOT NULL DEFAULT '2026-B',
    class_code TEXT UNIQUE,
    class_code_enable BOOLEAN DEFAULT TRUE,
    status INT2 DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 5. TABLA: STUDENT_ENROLLMENT
CREATE TABLE IF NOT EXISTS public.student_enrollment (
    student_id UUID REFERENCES public.student(student_id) ON DELETE CASCADE,
    course_catalog_id TEXT REFERENCES public.course_catalog(course_catalog_id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    PRIMARY KEY (student_id, course_catalog_id)
);

-- 6. TABLA: POST
CREATE TABLE IF NOT EXISTS public.post (
    post_id BIGSERIAL PRIMARY KEY,
    author_id UUID,
    author_type INT2 NOT NULL DEFAULT 1,
    author_name TEXT,
    email TEXT,
    course_catalog_id TEXT REFERENCES public.course_catalog(course_catalog_id) ON DELETE CASCADE,
    title TEXT,
    content TEXT,
    allow_comments BOOLEAN DEFAULT TRUE,
    trigger_timestamp TIMESTAMP,
    is_private BOOLEAN DEFAULT FALSE,
    post_id_ref BIGINT,
    receiver_private UUID,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 7. TABLA: COMMENT
CREATE TABLE IF NOT EXISTS public.comment (
    comment_id BIGSERIAL PRIMARY KEY,
    post_id BIGINT REFERENCES public.post(post_id) ON DELETE CASCADE,
    author_id UUID,
    author_type INT2 NOT NULL DEFAULT 1,
    author_name TEXT,
    email TEXT,
    content TEXT,
    comment_id_ref BIGINT,
    review_status INT2 DEFAULT 1,
    is_private BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 8. TABLA: USER_PROFILE
CREATE TABLE IF NOT EXISTS public.user_profile (
    user_profile_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    firstname TEXT,
    surname TEXT,
    short_name TEXT,
    email TEXT,
    user_type INT2 DEFAULT 1,
    status INT2 DEFAULT 1,
    is_full_profile BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- =============================================================================
-- VISTAS RELACIONALES (Requeridas por la App y Edge Functions)
-- =============================================================================

CREATE OR REPLACE VIEW public.student_enrollment_view AS
SELECT
    cc.course_catalog_id,
    c.course_id,
    c.code AS course_code,
    c.name AS course_name,
    cc.semester,
    cc.course_type,
    cc.group_type,
    p.firstname,
    p.surname1 AS surname,
    p.email,
    cc.class_code,
    cc.class_code_enable
FROM public.course_catalog cc
LEFT JOIN public.course c ON cc.course_id = c.course_id
LEFT JOIN public.professor p ON cc.professor_id = p.professor_id;

CREATE OR REPLACE VIEW public.course_catalog_view AS
SELECT
    cc.course_catalog_id,
    c.code AS course_code,
    c.name AS course_name
FROM public.course_catalog cc
LEFT JOIN public.course c ON cc.course_id = c.course_id;

CREATE OR REPLACE VIEW public.student_course_view AS
SELECT
    se.course_catalog_id,
    s.student_id,
    s.firstname,
    s.surname1 AS surname,
    s.email,
    s.codigo
FROM public.student_enrollment se
JOIN public.student s ON se.student_id = s.student_id;

-- =============================================================================
-- FUNCIÓN RPC: enroll_student_by_code
-- =============================================================================

CREATE OR REPLACE FUNCTION public.enroll_student_by_code(
    p_student_id UUID,
    p_code TEXT
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    v_catalog_id TEXT;
    v_is_open BOOLEAN;
    v_enrolled_count INT;
BEGIN
    p_code := UPPER(TRIM(p_code));

    SELECT course_catalog_id, COALESCE(class_code_enable, true)
    INTO v_catalog_id, v_is_open
    FROM public.course_catalog
    WHERE UPPER(class_code) = p_code OR UPPER(course_catalog_id) = p_code;

    IF v_catalog_id IS NULL THEN
        RETURN jsonb_build_object(
            'success', false,
            'message', 'El código no existe o las inscripciones están cerradas.',
            'already_enrolled', false,
            'course_id', NULL
        );
    END IF;

    IF v_is_open IS NOT TRUE THEN
        RETURN jsonb_build_object(
            'success', false,
            'message', 'Las inscripciones para este curso se encuentran cerradas.',
            'already_enrolled', false,
            'course_id', v_catalog_id
        );
    END IF;

    SELECT COUNT(*) INTO v_enrolled_count
    FROM public.student_enrollment
    WHERE student_id = p_student_id AND course_catalog_id = v_catalog_id;

    IF v_enrolled_count > 0 THEN
        RETURN jsonb_build_object(
            'success', true,
            'message', 'Ya perteneces a este curso.',
            'already_enrolled', true,
            'course_id', v_catalog_id
        );
    END IF;

    INSERT INTO public.student_enrollment (student_id, course_catalog_id, created_at)
    VALUES (p_student_id, v_catalog_id, NOW());

    RETURN jsonb_build_object(
        'success', true,
        'message', 'Te has incorporado exitosamente al curso.',
        'already_enrolled', false,
        'course_id', v_catalog_id
    );

EXCEPTION WHEN OTHERS THEN
    RETURN jsonb_build_object(
        'success', false,
        'message', 'Error interno al procesar la inscripción: ' || SQLERRM,
        'already_enrolled', false,
        'course_id', NULL
    );
END;
$$;

-- =============================================================================
-- DATOS INICIALES DE PRUEBA
-- =============================================================================

INSERT INTO public.student (student_id, firstname, surname1, email)
VALUES ('c5b12877-4122-43d8-b59a-129487563812', 'Juan', 'Pérez', 'estudiante@unsa.edu.pe')
ON CONFLICT (student_id) DO NOTHING;

INSERT INTO public.professor (professor_id, firstname, surname1, email)
VALUES ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'Julio', 'Pérez', 'julio@unsa.edu.pe')
ON CONFLICT (professor_id) DO NOTHING;

INSERT INTO public.course (course_id, name, code)
VALUES ('COURSE-101', 'Curso 1 - Programación Avanzada', '000001')
ON CONFLICT (course_id) DO NOTHING;

INSERT INTO public.course_catalog (course_catalog_id, professor_id, course_id, group_type, course_type, semester, class_code, class_code_enable)
VALUES ('CAT-101', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'COURSE-101', 'GRUPO_A', 'OBLIGATORIO', '2026-B', 'A8K92X', true)
ON CONFLICT (course_catalog_id) DO NOTHING;

-- =============================================================================
-- DESHABILITAR RLS EN TODAS LAS TABLAS
-- =============================================================================

ALTER TABLE public.student DISABLE ROW LEVEL SECURITY;
ALTER TABLE public.professor DISABLE ROW LEVEL SECURITY;
ALTER TABLE public.course DISABLE ROW LEVEL SECURITY;
ALTER TABLE public.course_catalog DISABLE ROW LEVEL SECURITY;
ALTER TABLE public.student_enrollment DISABLE ROW LEVEL SECURITY;
ALTER TABLE public.post DISABLE ROW LEVEL SECURITY;
ALTER TABLE public.comment DISABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_profile DISABLE ROW LEVEL SECURITY;
