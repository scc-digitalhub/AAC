import { TranslationMessages } from 'ra-core';
import germanMessages from 'ra-language-german';

const messages: TranslationMessages = {
    AAC: 'AAC',
    helloww: 'Hello!!',
    Accounts: 'Accounts',
    Credentials: 'Credentials',
    Connections: 'Connections',
    Welcome: 'Welcome',
    delete: 'Delete',
    cancel: 'Cancel',
    dashboard: {
        personal_information:
            'Manage you personal information, accounts and review your security settings.',
        review_manage: 'Review and manage your accounts',
        manage_accounts: 'Manage Accounts',
        view_update: 'View and update your credentials',
        edit_profile: 'Edit profile',
        active_sessions:
            'You have currently active sessions on one or more devices.',
        view_sessions: 'View sessions',
        view_and: 'view connections in applications',
        manage_connections: 'Manage Connections',
        credentials: 'Credentials',
        edit_credentials: 'Edit credentials',
    },
    account_page: {
        informations: 'informations',
        registered_user: 'Registered user',
        personal_info_reg: 'Your personal information as of registration',
        email_verified: 'Email verified',
        name: 'Name',
        given_name: 'Given name',
        family_name: 'Family name',
        email: 'Email',
        preferred_username: 'Preferred username',
        delete_account: 'Delete account',
        delete_account_info:
            'Remove your account and all the associated information',
        delete: 'Delete',
        linked_accounts: 'Linked accounts',
        zone_info: 'Zone info',
        locale: 'Locale',
        surname: 'Surname',
        username: 'Username',
        not_verified: 'Not verified',
        verified: 'Verified',
        confirm_delete:
            'Are you sure you want to delete your account? This action cannot be undone!',
    },
    edit_password: {
        password: 'Password',
        change_password: 'Change password',
        current_password: 'Current password',
        verify_password: 'Verify password',
    },
    error: {
        already_registered: 'Already registered.',
        authentication_service:
            'Authentication problem. Please try again later.',
        bad_credentials: 'Invalid user or password',
        duplicated_data: 'Duplicated data',
        internal_error: 'Internal error. Please try again.',
        invalid_data: 'Invalid field data.',
        invalid_email: 'Invalid email',
        //invalid_password: 'Invalid user or password',
        invalid_password: {
            contains_whitespace: 'Invalid password: contains whitespaces',
            empty: 'Invalid password: empty password',
            max_length: 'Invalid password: too long',
            min_length: 'Invalid password: too short',
            password_reuse: 'Invalid password: password reuse',
            require_alpha: 'Invalid password: alpha char required',
            require_number: 'Invalid password: number required',
            require_uppercase_alpha:
                'Invalid password: uppercase alpha char required',
            require_special: 'Invalid password: special character required',
            not_match: 'Ivalid password: the passwords do not match',
        },
        invalid_user: 'Invalid user',
        locked: 'User locked',
        mismatch_passwords: 'Passwords does not match',
        missing_data: 'Missing data',
        not_confirmed: 'Registration not confirmed.',
        not_registered: 'Invalid user or password',
        // registration: 'Registration problem. Please try again later.',
        invalid_field: 'Invalid field data.',
        unsupported_operation: 'Unsupported operation.',
        wrong_password: 'Wrong password',
        unauthenticated_user: 'User must be authenticated.',
        registration: {
            invalid_password: 'Invalid password',
        },
    },
    ...germanMessages,
};

export default messages;
