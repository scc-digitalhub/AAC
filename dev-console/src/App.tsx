import './App.css';
import { Admin, Resource, defaultTheme } from 'react-admin';
import { BrowserRouter } from 'react-router-dom';
import appDataProvider from './dataProvider';
import appAuthProvider from './authProvider';
import MyLayout from './components/layout';

import 'typeface-titillium-web';
import 'typeface-roboto-mono';

import {
    RootSelectorContextProvider,
} from '@dslab/ra-root-selector';

import DevDashboard from './pages/dashboard';

import { LoginPage } from './pages/login';
import 'ace-builds/src-noconflict/ace';

import apps from './apps';

import { AuditList } from './audit/AuditList';
import { AttributeSetList } from './attributeset/AttributeSetList';
import { IdpList } from './idps/IdpList';
import { IdpCreate } from './idps/IdpCreate';
import i18nProvider from './i18nProvider';
import { IdpEdit } from './idps/IdpEdit';
import { ServiceList } from './service/ServiceList';
import { ServiceCreate } from './service/ServiceCreate';
import { UserList } from './users/UserList';
import { UserCreate } from './users/UserCreate';
import { TemplateList } from './templates/TemplateList';
import { ScopeList } from './scopes/ScopeList';
import { GroupList } from './group/GroupList';
import { GroupCreate } from './group/GroupCreate';
import { GroupEdit } from './group/GroupEdit';
import { UserShow } from './users/UserShow';
import { RealmIcon } from './myrealms/RealmIcon';
import audit from './audit';
import service from './service';
import users from './users';
import { group } from 'console';
import scopes from './scopes';
import templates from './templates';
import { IdpIcon } from './idps/IdpIcon';
import { UserIcon } from './users/UserIcon';
import { GroupShow } from './group/GroupShow';
import { GroupIcon } from './group/GroupIcon';
import { RecourceIcon } from './resources/ResourceIcon';

//config
const CONTEXT_PATH: string =
    import.meta.env.BASE_URL ||
    (globalThis as any).REACT_APP_CONTEXT_PATH ||
    (process.env.REACT_APP_CONTEXT_PATH as string);
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

const DevApp = () => {
    return (
        <RootSelectorContextProvider
            resource="myrealms"
            // initialApp={<InitialWrapper />}
        >
            <Admin
                dataProvider={dataProvider}
                authProvider={authProvider}
                i18nProvider={i18nProvider}
                dashboard={DevDashboard}
                layout={MyLayout}
                theme={myTheme}
                loginPage={<LoginPage />}
                authCallbackPage={false}
                requireAuth
                disableTelemetry
            >
                <Resource name="apps" {...apps} />
                <Resource name="idps" list={<IdpList/>} create={<IdpCreate/>} edit={<IdpEdit/>}  icon={IdpIcon}/>
                <Resource name="myrealms" icon={RealmIcon} />
                <Resource name="audit" list={<AuditList/>}  />
                <Resource name="services" list={<ServiceList/>}  create={<ServiceCreate/>} />
                <Resource name="users" list={<UserList/>}  create={<UserCreate/>} show={<UserShow />} icon={UserIcon}/>
                <Resource name="groups" list={<GroupList/>}  create={<GroupCreate/>} edit={ <GroupEdit />} show={<GroupShow />} icon={GroupIcon}/>
                <Resource name="resources" list={<ScopeList/>}  icon={RecourceIcon} />
                <Resource name="attributeset" list={<AttributeSetList/>}  />
                <Resource name="templates" list={<TemplateList/>}  />             
            </Admin>
        </RootSelectorContextProvider>
    );
};

export const App = () => {
    return (
        <BrowserRouter basename={CONTEXT_PATH}>
            <DevApp />
        </BrowserRouter>
    );
};
