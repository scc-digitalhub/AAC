import {
    AppBar,
    AppBarProps,
    Layout,
    useGetIdentity,
    useTranslate,
} from 'react-admin';
import { Typography } from '@mui/material';
import { FunctionComponent } from 'react';
import { Logout, UserMenu } from 'react-admin';
import MenuItem from '@mui/material/MenuItem';
import { MenuItemProps } from '@mui/material/MenuItem';
import ListItemIcon from '@mui/material/ListItemIcon';
import ListItemText from '@mui/material/ListItemText';
import DeveloperModeIcon from '@mui/icons-material/DeveloperMode';
import SettingsIcon from '@mui/icons-material/Settings';
import React from 'react';

import { RootResourceSelectorMenu } from '@dslab/ra-root-selector';
import { MyMenu } from './MyMenu';

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

const AppLayout = (props: any) => (
    <Layout {...props} appBar={MyAppBar} menu={MyMenu} />
);

export default AppLayout;
