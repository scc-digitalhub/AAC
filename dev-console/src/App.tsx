import './App.css';
import { Admin, Resource } from 'react-admin';
import { BrowserRouter } from 'react-router-dom';
import appDataProvider from './dataProvider';
import appAuthProvider from './authProvider';
import MyLayout from './components/MyLayout';

import 'typeface-titillium-web';
import 'typeface-roboto-mono';

import theme from './themeProvider';
import { RootSelectorContextProvider } from '@dslab/ra-root-selector';

import DevDashboard from './pages/dashboard';
import { LoginPage } from './pages/login';

import apps from './apps';
import groups from './group';
import roles from './roles';

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
import { ScopeList } from './scopes/ScopeList';

import { UserShow } from './users/UserShow';
import { RealmIcon } from './myrealms/RealmIcon';
import { IdpIcon } from './idps/IdpIcon';
import { UserIcon } from './users/UserIcon';

import { ApiResourceIcon } from './resources/ApiResourceIcon';
import { ServiceShow } from './service/ServiceShow';
import { ServiceEdit } from './service/ServiceEdit';
import { ServiceIcon } from './service/ServiceIcon';
import { AttributeIcon } from './attributeset/AttributeIcon';

import { IdpShow } from './idps/IdpShow';
import { UserEdit } from './users/UserEdit';

//config
const CONTEXT_PATH: string =
    import.meta.env.BASE_URL ||
    (globalThis as any).REACT_APP_CONTEXT_PATH ||
    (process.env.REACT_APP_CONTEXT_PATH as string);
const API_URL: string = process.env.REACT_APP_API_URL as string;
const dataProvider = appDataProvider(API_URL);
const authProvider = appAuthProvider(API_URL);

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
                theme={theme}
                loginPage={<LoginPage />}
                authCallbackPage={false}
                requireAuth
                disableTelemetry
            >
                <Resource name="apps" {...apps} />
                <Resource name="groups" {...groups} />
                <Resource name="roles" {...roles} />
                <Resource
                    name="idps"
                    list={<IdpList />}
                    create={<IdpCreate />}
                    edit={<IdpEdit />}
                    show={<IdpShow />}
                    icon={IdpIcon}
                />
                <Resource name="myrealms" icon={RealmIcon} />
                <Resource name="audit" list={<AuditList />} />
                <Resource
                    name="services"
                    list={<ServiceList />}
                    create={<ServiceCreate />}
                    show={<ServiceShow />}
                    edit={<ServiceEdit />}
                    icon={ServiceIcon}
                />
                <Resource
                    name="users"
                    list={<UserList />}
                    create={<UserCreate />}
                    show={<UserShow />}
                    edit={<UserEdit />}
                    icon={UserIcon}
                />

                <Resource
                    name="resources"
                    list={<ScopeList />}
                    icon={ApiResourceIcon}
                />
                <Resource
                    name="scopes"
                    list={<ScopeList />}
                    recordRepresentation={record => `${record.name}`}
                />

                <Resource
                    name="attributeset"
                    list={<AttributeSetList />}
                    icon={AttributeIcon}
                />
                <Resource name="subjects" />
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
