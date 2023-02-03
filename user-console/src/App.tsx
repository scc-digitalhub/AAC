import React from 'react';
import logo from './logo.svg';
import './App.css';
import {
    Admin,
    CustomRoutes,
    defaultTheme,
    ListGuesser,
    Resource,
} from 'react-admin';
import { Route } from 'react-router-dom';
import appDataProvider from './dataProvider';
import appAuthProvider from './authProvider';
import i18nProvider from './i18nProvider';
import { AccountEdit, AccountList } from './resources/accounts';
import MyLayout from './components/layout';

import 'typeface-titillium-web';
import 'typeface-roboto-mono';
import 'typeface-lora';

import GroupIcon from '@mui/icons-material/Group';
import AccountBoxIcon from '@mui/icons-material/AccountBox';
import VpnKeyIcon from '@mui/icons-material/VpnKey';
import LockIcon from '@mui/icons-material/Lock';
import AppShortcutIcon from '@mui/icons-material/AppShortcut';

import UserDashboard from './pages/dashboard';
import { ProfilesList } from './resources/profiles';
import { ConnectionsList } from './resources/connections';
import { SecurityPage } from './pages/security';
import { CredentialEdit, CredentialsList } from './resources/credentials';

const dataProvider = appDataProvider('http://localhost:9090');
const authProvider = appAuthProvider('http://localhost:9090');

const myTheme = {
    ...defaultTheme,
    palette: {
        primary: {
            main: '#0066cc',
            dark: '#00478e',
            light: '#3384d6',
        },
        secondary: {
            main: '#b2b2b2',
            dark: '#7c7c7c',
            light: '#c1c1c1',
        },
    },
    typography: {
        fontFamily: ['"Titillium Web"', 'Geneva', 'Tahoma', 'sans-serif'].join(
            ','
        ),
        h1: {
            fontWeight: 700,
        },
        h2: {
            fontWeight: 700,
        },
        h3: {
            fontWeight: 700,
        },
        h4: {
            fontWeight: 700,
        },
        h5: {
            fontWeight: 700,
        },
        button: {
            fontFamily: [
                '"Titillium Web"',
                'Geneva',
                'Tahoma',
                'sans-serif',
            ].join(','),
            fontWeight: 600,
            textTransform: 'none' as const,
            fontSize: '0.90rem',
        },
        body1: {
            // fontFamily: ['Lora', 'serif'].join(','),
        },
        body2: {
            // fontFamily: ['Lora', 'serif'].join(','),
        },
        // Use the system font instead of the default Roboto font.
        // fontFamily: ['-apple-system', 'BlinkMacSystemFont', '"Segoe UI"', 'Arial', 'sans-serif'].join(','),
    },
    components: {
        // Name of the component
        MuiCardContent: {
            styleOverrides: {
                // Name of the slot
                root: {
                    fontFamily: 'Lora,serif' as const,
                },
            },
        },
    },
};

const App = () => (
    <Admin
        dataProvider={dataProvider}
        authProvider={authProvider}
        i18nProvider={i18nProvider}
        dashboard={UserDashboard}
        layout={MyLayout}
        theme={myTheme}
    >
        <Resource
            name="accounts"
            icon={GroupIcon}
            list={AccountList}
            edit={AccountEdit}
        />
        <Resource name="profiles" icon={AccountBoxIcon} list={ProfilesList} />
        <Resource
            name="credentials"
            icon={VpnKeyIcon}
            list={CredentialsList}
            edit={CredentialEdit}
        />

        <Resource
            name="connections"
            icon={AppShortcutIcon}
            list={ConnectionsList}
        />
        <CustomRoutes>
            <Route path="/security" element={<SecurityPage />} />
        </CustomRoutes>
        <Resource name="details" />
        <Resource name="scopes" />
        {/* <Resource name="sessions" icon={LockIcon} list={ListGuesser} /> */}
        <Resource name="tokens" />
        <Resource name="audit" />
    </Admin>
);

export default App;
