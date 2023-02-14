import {
    AppBar,
    AppBarProps,
    Layout,
    LayoutProps,
    MenuProps,
    useGetIdentity,
    useTranslate,
} from 'react-admin';
import { Box, Typography } from '@mui/material';
import { Menu } from 'react-admin';
import { FunctionComponent } from 'react';

import LockIcon from '@mui/icons-material/Lock';
import AccountBoxIcon from '@mui/icons-material/AccountBox';
import VpnKeyIcon from '@mui/icons-material/VpnKey';
import AppShortcutIcon from '@mui/icons-material/AppShortcut';

import { Logout, UserMenu } from 'react-admin';
import MenuItem from '@mui/material/MenuItem';
import { MenuItemProps } from '@mui/material/MenuItem';

import ListItemIcon from '@mui/material/ListItemIcon';
import ListItemText from '@mui/material/ListItemText';
import DeveloperModeIcon from '@mui/icons-material/DeveloperMode';
import GroupIcon from '@mui/icons-material/Group';
import SettingsIcon from '@mui/icons-material/Settings';

import React from 'react';

const DEV_URL: string = process.env.REACT_APP_DEVELOPER_CONSOLE as string;
const ADMIN_URL: string = process.env.REACT_APP_ADMIN_CONSOLE as string;
const USER_URL: string = process.env.REACT_APP_USER_CONSOLE as string;

const DevConsoleMenu: FunctionComponent<MenuItemProps<'li'>> = React.forwardRef(
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

const UserConsoleMenu: FunctionComponent<MenuItemProps<'li'>> =
    React.forwardRef(function MenuToUser(props, ref) {
        const translate = useTranslate();
        const isXSmall = false;
        const handleClick = () => {
            window.location.href = USER_URL;
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
                <ListItemText>{translate('user')}</ListItemText>
            </MenuItem>
        );
    });

const MyUserMenu = () => {
    const { data } = useGetIdentity();

    const isDeveloper =
        data &&
        data.authorities.find(
            (r: any) =>
                r.role &&
                (r.role === 'ROLE_DEVELOPER' || r.role === 'ROLE_ADMIN')
        );

    return (
        <UserMenu>
            {isDeveloper && <DevConsoleMenu />}
            <UserConsoleMenu />
            <Logout />
        </UserMenu>
    );
};

const MyAppBar = (props: AppBarProps) => {
    const title = 'AAC';
    return (
        <AppBar {...props} color="primary" userMenu={<MyUserMenu />}>
            <Box flex="1">
                <Typography
                    variant="h6"
                    noWrap
                    component="a"
                    href="/"
                    sx={{
                        mr: 2,
                        flexGrow: 1,
                        display: { xs: 'none', sm: 'block' },
                        fontFamily: 'monospace',
                        fontWeight: 500,
                        // letterSpacing: '.3rem',
                        color: 'inherit',
                        textDecoration: 'none',
                        textTransform: 'uppercase',
                    }}
                >
                    {title}
                </Typography>
            </Box>
        </AppBar>
    );
};

const MyLayout = (props: LayoutProps) => (
    <Layout {...props} appBar={MyAppBar} />
);

export default MyLayout;
