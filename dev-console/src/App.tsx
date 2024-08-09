import { Admin, Resource } from 'react-admin';
import { BrowserRouter } from 'react-router-dom';

import appDataProvider from './dataProvider';
import appAuthProvider from './authProvider';
import i18nProvider from './i18nProvider';
import themeProvider from './themeProvider';
import './App.css';
import AppLayout from './components/AppLayout';

import {
    RootSelectorContextProvider,
    RootSelectorInitialWrapper,
} from '@dslab/ra-root-selector';

// pages
import DevDashboard from './pages/dashboard';
import { LoginPage } from './pages/login';

// resources
import apps from './apps';
import groups from './group';
import roles from './roles';
import idps from './idps';
import apiResources from './resources';
import myrealms from './myrealms';

import { AuditList } from './audit/AuditList';
import { AttributeSetList } from './attributeset/AttributeSetList';

import { ServiceList } from './service/ServiceList';
import { ServiceCreate } from './service/ServiceCreate';
import { UserList } from './users/UserList';
import { UserCreate } from './users/UserCreate';
import { ScopeList } from './scopes/ScopeList';

import { UserShow } from './users/UserShow';
import { RealmIcon } from './myrealms/RealmIcon';
import { UserIcon } from './users/UserIcon';

import { ServiceShow } from './service/ServiceShow';
import { ServiceEdit } from './service/ServiceEdit';
import { ServiceIcon } from './service/ServiceIcon';
import { AttributeIcon } from './attributeset/AttributeIcon';

import { UserEdit } from './users/UserEdit';
import { RealmList, RealmSelectorList } from './myrealms/RealmList';

//config
const CONTEXT_PATH: string =
    import.meta.env.BASE_URL ||
    (globalThis as any).REACT_APP_CONTEXT_PATH ||
    (process.env.REACT_APP_CONTEXT_PATH as string);
const API_URL: string = process.env.REACT_APP_API_URL as string;
const dataProvider = appDataProvider(API_URL);
const authProvider = appAuthProvider(API_URL);
export const DEFAULT_LANGUAGES = ['en', 'it', 'es', 'lv', 'de'];

const DevApp = () => {
    return (
        <RootSelectorContextProvider
            resource="myrealms"
            initialApp={<InitialWrapper />}
        >
            <Admin
                dataProvider={dataProvider}
                authProvider={authProvider}
                i18nProvider={i18nProvider}
                dashboard={DevDashboard}
                layout={AppLayout}
                theme={themeProvider}
                loginPage={<LoginPage />}
                authCallbackPage={false}
                requireAuth
                disableTelemetry
            >
                <Resource name="apps" {...apps} />
                <Resource name="groups" {...groups} />
                <Resource name="roles" {...roles} />
                <Resource name="idps" {...idps} />
                <Resource name="myrealms" {...myrealms} />
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

                <Resource name="resources" {...apiResources} />
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
const InitialWrapper = () => {
    return (
        <RootSelectorInitialWrapper
            resource="myrealms"
            selector={<RealmSelectorList />}
        >
            <Admin
                layout={AppLayout}
                dataProvider={dataProvider}
                authProvider={authProvider}
                i18nProvider={i18nProvider}
                theme={themeProvider}
                loginPage={<LoginPage />}
                authCallbackPage={false}
                requireAuth
                disableTelemetry
            >
                <Resource name="myrealms" {...myrealms} />
            </Admin>
        </RootSelectorInitialWrapper>
    );
};

export const App = () => {
    return (
        <BrowserRouter basename={CONTEXT_PATH}>
            <DevApp />
        </BrowserRouter>
    );
};
