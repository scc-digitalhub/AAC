import {
    AppBar,
    AppBarProps,
    Layout,
    useGetIdentity,
    useTranslate,
    useCreatePath,
    useGetResourceLabel,
    useResourceDefinitions,
    useStore,
    Menu,
    Button,
    useBasename,
    MenuItemLink,
} from 'react-admin';
import { useLocation } from 'react-router-dom';
import { Box, Typography } from '@mui/material';
import { FunctionComponent } from 'react';
import VpnKeyIcon from '@mui/icons-material/VpnKey';
import { Logout, UserMenu } from 'react-admin';
import MenuItem from '@mui/material/MenuItem';
import { MenuItemProps } from '@mui/material/MenuItem';
import ListItemIcon from '@mui/material/ListItemIcon';
import ListItemText from '@mui/material/ListItemText';
import DeveloperModeIcon from '@mui/icons-material/DeveloperMode';
import AppsIcon from '@mui/icons-material/Apps';
import MiscellaneousServicesIcon from '@mui/icons-material/MiscellaneousServices';
import GroupIcon from '@mui/icons-material/Group';
import GroupsIcon from '@mui/icons-material/Groups';
import LockOpenIcon from '@mui/icons-material/LockOpen';
import GradingIcon from '@mui/icons-material/Grading';
import SettingsIcon from '@mui/icons-material/Settings';
import ListAltIcon from '@mui/icons-material/ListAlt';
import DatasetIcon from '@mui/icons-material/Dataset';
import PasswordIcon from '@mui/icons-material/Password';
import React from 'react';
import { RealmListMenu } from './realmListMenu';
import { Divider } from '@mui/material';

import {
    RootResourceSelectorMenu,
    useRootSelector,
} from '@dslab/ra-root-selector';
import { DashboardRounded } from '@mui/icons-material';
import { RealmIcon } from '../myrealms/RealmIcon';

const DEV_URL: string = process.env.REACT_APP_DEVELOPER_CONSOLE as string;
const ADMIN_URL: string = process.env.REACT_APP_ADMIN_CONSOLE as string;

const DeveloperMenu: FunctionComponent<MenuItemProps<'li'>> = React.forwardRef(
    function MenuToDev(props, ref) {
        const translate = useTranslate();
        const isXSmall = false;
        const handleClick = () => {
            window.location.href = DEV_URL;
            return;
        };
        return (
            <MenuItem
                onClick={handleClick}
                ref={ref}
                component={isXSmall ? 'span' : 'li'}
            >
                <ListItemIcon>
                    <DeveloperModeIcon />
                </ListItemIcon>
                <ListItemText>{translate('developer')}</ListItemText>
            </MenuItem>
        );
    }
);

const AdminMenu: FunctionComponent<MenuItemProps<'li'>> = React.forwardRef(
    function MenuToAdmin(props, ref) {
        const translate = useTranslate();
        const isXSmall = false;
        const handleClick = () => {
            window.location.href = ADMIN_URL;
            return;
        };
        return (
            <MenuItem
                onClick={handleClick}
                ref={ref}
                component={isXSmall ? 'span' : 'li'}
            >
                <ListItemIcon>
                    <SettingsIcon />
                </ListItemIcon>
                <ListItemText>{translate('admin')}</ListItemText>
            </MenuItem>
        );
    }
);

const MyUserMenu = () => {
    const { data } = useGetIdentity();

    const isDeveloper =
        data &&
        data.authorities.find(
            (r: any) =>
                r.role &&
                (r.role === 'ROLE_DEVELOPER' || r.role === 'ROLE_ADMIN')
        );
    const isAdmin =
        data && data.authorities.find((r: any) => r.authority === 'ROLE_ADMIN');

    return (
        <UserMenu>
            {isDeveloper && <DeveloperMenu />}
            {isAdmin && <AdminMenu />}
            <Logout />
        </UserMenu>
    );
};

const MyAppBar = (props: AppBarProps) => {
    return (
        <AppBar {...props} color="primary" userMenu={<MyUserMenu />}>
            <Typography
                flex="1"
                textOverflow="ellipsis"
                whiteSpace="nowrap"
                overflow="hidden"
                variant="h6"
                color="inherit"
            >
                <RootResourceSelectorMenu
                    source="name"
                    showSelected={true}
                    icon={false}
                />
            </Typography>
        </AppBar>
    );
};

export const MyMenu = () => {
    const basename = useBasename();
    const getResourceLabel = useGetResourceLabel();
    const { base, root } = useRootSelector();
    console.log('base', base);
    return (
        <Menu
            sx={{
                height: '100%',
                pt: '18px',
            }}
        >
            <Box flex={1}>
                <Menu.DashboardItem />
                <Menu.ResourceItem name="apps" />
                <Menu.ResourceItem name="idps" />
                <Menu.ResourceItem name="audit" />
                <Menu.ResourceItem name="sevices" />
                <Menu.ResourceItem name="users" />
                <Menu.ResourceItem name="groups" />
                <Menu.ResourceItem name="resources" />
                <Menu.ResourceItem name="templates" />
                <Menu.ResourceItem name="services" />
                <Menu.ResourceItem name="roles" />
                <Menu.ResourceItem name="attributeset" />
                <Divider />
                <MenuItemLink
                    leftIcon={<SettingsIcon />}
                    to={`${basename}/config`}
                    primaryText={'menu.configuration'}
                />
                <MenuItemLink
                    leftIcon={<RealmIcon />}
                    to={base || '/'}
                    primaryText={<>{getResourceLabel('myrealms', 2)}</>}
                    selected={false}
                />
            </Box>
        </Menu>
    );
};

const MyMenu2 = (props: any) => {
    let url = useLocation();
    const regDomain = new RegExp('(?<=(/r/|/domains/))([^/]+)');
    const realmId = regDomain.test(url?.pathname)
        ? url?.pathname?.match(regDomain)![0] !== 'create'
            ? url?.pathname?.match(regDomain)![0]
            : ''
        : '';

    // const resources = useResourceDefinitions();
    // const getResourceLabel = useGetResourceLabel();
    // const createPath = useCreatePath();
    // const listHiddenMenu = [
    //     'myrealms',
    //     'App and services',
    //     'User and groups',
    //     'Audit',
    // ];

    // let links = Object.keys(resources)
    //     .filter(name => !listHiddenMenu.includes(name))
    //     .map(name => {
    //         return (
    //             <MenuItemLink
    //                 key={name}
    //                 to={createPath({ resource: name, type: 'list' })}
    //                 // state={{ _scrollToTop: true }}
    //                 primaryText={getResourceLabel(name, 2)}
    //             />
    //         );
    //     });

    return (
        <>
            {realmId !== 'system' && (
                <Menu>
                    {realmId && <Menu.DashboardItem />}
                    {realmId && (
                        <Menu.Item
                            to={`/apps/r/${realmId}`}
                            primaryText="Apps"
                            leftIcon={<AppsIcon />}
                        />
                    )}
                    {realmId && (
                        <Menu.Item
                            to={`/services/r/${realmId}`}
                            primaryText="Services"
                            leftIcon={<MiscellaneousServicesIcon />}
                        />
                    )}
                    {realmId && (
                        <Menu.Item
                            to={`/users/r/${realmId}`}
                            primaryText="Users"
                            leftIcon={<GroupIcon />}
                        />
                    )}
                    {realmId && (
                        <Menu.Item
                            to={`/groups/r/${realmId}`}
                            primaryText="Groups"
                            leftIcon={<GroupsIcon />}
                        />
                    )}
                    {realmId && (
                        <Menu.Item
                            to={`/idps/r/${realmId}`}
                            primaryText="Authentication"
                            leftIcon={<VpnKeyIcon />}
                        />
                    )}
                    {realmId && (
                        <Menu.Item
                            to={`/roles/r/${realmId}`}
                            primaryText="Authorization roles"
                            leftIcon={<LockOpenIcon />}
                        />
                    )}
                    {realmId && (
                        <Menu.Item
                            to={`/resources/r/${realmId}`}
                            primaryText="Authorization scopes"
                            leftIcon={<PasswordIcon />}
                        />
                    )}
                    {realmId && (
                        <Menu.Item
                            to={`/aps/r/${realmId}`}
                            primaryText="Attributes providers"
                            leftIcon={<ListAltIcon />}
                        />
                    )}
                    {realmId && (
                        <Menu.Item
                            to={`/attributeset/r/${realmId}`}
                            primaryText="Attributes sets"
                            leftIcon={<DatasetIcon />}
                        />
                    )}
                    {realmId && (
                        <Menu.Item
                            to={`/audit/r/${realmId}`}
                            primaryText="Audit"
                            leftIcon={<GradingIcon />}
                        />
                    )}
                    {realmId && (
                        <Menu.Item
                            to={`/templates/r/${realmId}`}
                            primaryText="Configuration"
                            leftIcon={<SettingsIcon />}
                        />
                    )}
                </Menu>
            )}

            {realmId === 'system' && (
                <Menu>
                    <Menu.DashboardItem />
                    <Menu.Item
                        to={`/apps/r/${realmId}`}
                        primaryText="Apps"
                        leftIcon={<AppsIcon />}
                    />
                    <Menu.Item
                        to={`/services/r/${realmId}`}
                        primaryText="Services"
                        leftIcon={<MiscellaneousServicesIcon />}
                    />
                    <Menu.Item
                        to={`/users/r/${realmId}`}
                        primaryText="Users"
                        leftIcon={<GroupIcon />}
                    />
                    <Menu.Item
                        to={`/groups/r/${realmId}`}
                        primaryText="Groups"
                        leftIcon={<GroupsIcon />}
                    />
                    <Menu.Item
                        to={`/audit/r/${realmId}`}
                        primaryText="Audit"
                        leftIcon={<GradingIcon />}
                    />
                </Menu>
            )}
        </>
    );
};

const MyLayout = (props: any) => (
    <Layout {...props} appBar={MyAppBar} menu={MyMenu} />
);

export default MyLayout;
