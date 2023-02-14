import './App.css';
import {
    Admin,
    CustomRoutes,
    defaultTheme,
    EditGuesser,
    ListGuesser,
    Resource,
} from 'react-admin';
import { Route } from 'react-router-dom';
import appDataProvider from './dataProvider';
import appAuthProvider from './authProvider';
import i18nProvider from './i18nProvider';
import MyLayout from './components/layout';

import 'typeface-titillium-web';
import 'typeface-roboto-mono';

import GroupIcon from '@mui/icons-material/Group';
import AccountBoxIcon from '@mui/icons-material/AccountBox';
import VpnKeyIcon from '@mui/icons-material/VpnKey';
import AppShortcutIcon from '@mui/icons-material/AppShortcut';
import WorkspacesIcon from '@mui/icons-material/Workspaces';

import AdminDashboard from './pages/dashboard';
import { RealmsPage } from './pages/realms';

import { LoginPage } from './pages/login';
import { RealmCreate, RealmEdit } from './resources/realms';

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
        i18nProvider={i18nProvider}
        dashboard={AdminDashboard}
        layout={MyLayout}
        theme={myTheme}
        loginPage={<LoginPage />}
        authCallbackPage={false}
        requireAuth
        disableTelemetry
    >
        <Resource
            name="realms"
            icon={WorkspacesIcon}
            list={RealmsPage}
            edit={RealmEdit}
            create={RealmCreate}
        />
    </Admin>
);

export default App;
