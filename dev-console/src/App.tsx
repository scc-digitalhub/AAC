import './App.css';
import {
    Admin,
    Resource,
    ListGuesser,
    defaultTheme,
    CustomRoutes,
} from 'react-admin';
import { Route } from 'react-router-dom';
import appDataProvider from './dataProvider';
import appAuthProvider from './authProvider';
// import i18nProvider from './i18nProvider';
import MyLayout from './components/layout';

import 'typeface-titillium-web';
import 'typeface-roboto-mono';

import GroupIcon from '@mui/icons-material/Group';
import AccountBoxIcon from '@mui/icons-material/AccountBox';
import VpnKeyIcon from '@mui/icons-material/VpnKey';
import AppShortcutIcon from '@mui/icons-material/AppShortcut';

import UserDashboard from './pages/dashboard';
import { ConnectionsPage } from './pages/connections';
import { SecurityPage } from './pages/security';
import { CredentialsPage } from './pages/credentials';
import { AccountsPage } from './pages/accounts';
import { AccountEdit } from './resources/accounts';

import { PasswordCreate, PasswordEdit } from './resources/password';
import { WebAuthnCreate, WebAuthnEdit } from './resources/webauthn';
import { ProfilesPage } from './pages/profiles';
import { LoginPage } from './pages/login';
import myrealms from './myrealms';
import myaudit from './audit';
import { AuditList } from './audit/AuditList';
import { DebugList } from './components/DebugList';
import { AppList } from './apps/AppList';
import { RealmList } from './myrealms/RealmList';
import { RealmEdit } from './myrealms/RealmEdit';
import { RealmCreate } from './myrealms/RealmCreate';
import { AttributeSetList } from './attributeset/AttributeSetList';
import { AppShow } from './apps/AppShow';
import { AppCreate } from './apps/AppCreate';
import { AppEdit } from './apps/AppEdit';

const API_URL: string = process.env.REACT_APP_API_URL as string;
const dataProvider = appDataProvider(API_URL);
const authProvider = appAuthProvider(API_URL);

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
                    // fontFamily: 'Lora,serif' as const,
                },
            },
        },
    },
};

const App = () => (
    <Admin
        dataProvider={dataProvider}
        authProvider={authProvider}
        // i18nProvider={i18nProvider}
        // dashboard={UserDashboard}
        layout={MyLayout}
        theme={myTheme}
        loginPage={<LoginPage />}
        authCallbackPage={false}
        requireAuth
        disableTelemetry
    >
        {/* <Resource name="myrealms" {...myrealms} /> */}

        <Resource name="myrealms" hasCreate={true} create={RealmCreate}>
            <Route path="*" element={<RealmList />} />
            <Route path="create/*" element={<RealmCreate />} />
            <Route path=":id/edit/*" element={<RealmEdit />} />
        </Resource>
        <Resource name="audit">
            <Route path="/r/:realmId/*" element={<AuditList />} />
        </Resource>
        <Resource name="apps" hasCreate={true} create={AppCreate}>
            <Route path="/r/:realmId/*" element={<AppList />} />
            <Route path="/r/:realmId/:id" element={<AppShow />} />
            <Route path="/r/:realmId/:id/edit/*" element={<AppEdit />} />
            <Route path="/r/:realmId/create/*" element={<AppCreate />} />
        </Resource>
        <Resource name="services">
            <Route path="/r/:realmId/*" element={<DebugList />} />
        </Resource>
        <Resource name="users">
            <Route path="/r/:realmId/*" element={<DebugList />} />
        </Resource>
        <Resource name="groups">
            <Route path="/r/:realmId/*" element={<DebugList />} />
        </Resource>
        <Resource name="idps">
            <Route path="/r/:realmId/*" element={<DebugList />} />
        </Resource>
        <Resource name="roles">
            <Route path="/r/:realmId/*" element={<DebugList />} />
        </Resource>
        <Resource name="resources">
            <Route path="/r/:realmId/*" element={<DebugList />} />
        </Resource>
        <Resource name="aps">
            <Route path="/r/:realmId/*" element={<DebugList />} />
        </Resource>
        <Resource name="attributeset">
            <Route path="/r/:realmId/*" element={<AttributeSetList />} />
        </Resource>
        <Resource name="templates">
            <Route path="/r/:realmId/*" element={<DebugList />} />
        </Resource>
        <CustomRoutes>
            <Route path="/dashboard/r/:realmId/*" element={<UserDashboard />} />
        </CustomRoutes>

        {/* list={ListGuesser} */}
    </Admin>
);

export default App;
