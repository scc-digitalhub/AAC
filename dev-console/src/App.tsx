import { Admin, Resource } from 'react-admin';
import { BrowserRouter } from 'react-router-dom';

import appDataProvider from './dataProvider';
import appAuthProvider from './authProvider';
import i18nProvider from './i18nProvider';
import themeProvider from './themeProvider';
import './App.css';
import AppLayout from './layout/AppLayout';

import {
    RootSelectorContextProvider,
    RootSelectorInitialWrapper,
} from '@dslab/ra-root-selector';

// pages
import DevDashboard from './pages/dashboard';
import { LoginPage } from './pages/login';

// resources
import myrealmsDefinition from './myrealms';
import appsDefinition from './apps';
import groupsDefinition from './group';
import rolesDefinition from './roles';
import usersDefinition from './users';
import idpsDefinition from './idps';
import apsDefinition from './aps';
import apiResourcesDefinition from './resources';
import auditDefinition from './audit';
import serviceDefinition from './service';
import attributeSetDefinition from './attributeset';
import scopesDefinition from './scopes';

import { RealmSelectorList } from './myrealms/RealmList';

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
            resource={myrealmsDefinition.name}
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
                <Resource {...appsDefinition} />
                <Resource {...groupsDefinition} />
                <Resource {...rolesDefinition} />
                <Resource {...idpsDefinition} />
                <Resource {...apsDefinition} />
                <Resource
                    name={myrealmsDefinition.name}
                    edit={myrealmsDefinition.edit}
                    recordRepresentation={
                        myrealmsDefinition.recordRepresentation
                    }
                    icon={myrealmsDefinition.icon}
                />
                <Resource {...auditDefinition} />
                <Resource {...serviceDefinition} />
                <Resource {...usersDefinition} />

                <Resource {...apiResourcesDefinition} />
                <Resource {...scopesDefinition} />
                <Resource {...attributeSetDefinition} />
                <Resource name="subjects" />
                <Resource name="connectedapps" />
                <Resource name="developers" />
            </Admin>
        </RootSelectorContextProvider>
    );
};
const InitialWrapper = () => {
    return (
        <RootSelectorInitialWrapper
            resource={myrealmsDefinition.name}
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
                <Resource
                    name={myrealmsDefinition.name}
                    list={myrealmsDefinition.list}
                    icon={myrealmsDefinition.icon}
                    recordRepresentation={
                        myrealmsDefinition.recordRepresentation
                    }
                />
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
