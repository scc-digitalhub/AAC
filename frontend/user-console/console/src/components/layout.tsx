import {
    AppBar,
    AppBarProps,
    Layout,
    LayoutProps,
    MenuProps,
    useGetIdentity,
    useSidebarState,
    useTranslate,
} from 'react-admin';
import { Avatar, Box, CardHeader, Typography } from '@mui/material';
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
import { PageTitle } from './pageTitle';
import { UserAvatar } from './userAvatar';

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
    const { data, isLoading } = useGetIdentity();
    let title = 'AAC';
    if (!isLoading && data && data.realm) {
        title = data.realm.name;
    }

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

const MyMenu = (props: MenuProps) => {
    // const resources = useResourceDefinitions();
    // const links = Object.keys(resources)
    //     .filter(name => resources[name].hasList)
    //     .map(name => {
    //         return <Menu.ResourceItem key={name} name={name} />;
    //     });
    const translate = useTranslate();
    const { data, isLoading } = useGetIdentity();
    const [open, setOpen] = useSidebarState();

    return (
        <>
            {' '}
            {!isLoading && data && (
                <CardHeader
                    sx={{ mt: 2, pb: 0, pl: '12px' }}
                    avatar={<UserAvatar size="small" user={data} />}
                    title={data.fullName}
                    subheader={data.username}
                    onClick={() => setOpen(!open)}
                />
            )}
            <Menu sx={{ mt: 0 }}>
                <Menu.DashboardItem />
                {/* {links} */}
                <Menu.Item
                    to="/accounts"
                    primaryText="accounts"
                    leftIcon={<GroupIcon />}
                />
                <Menu.Item
                    to="/profiles"
                    primaryText="profiles"
                    leftIcon={<AccountBoxIcon />}
                />
                <Menu.Item
                    to="/credentials"
                    primaryText="credentials"
                    leftIcon={<VpnKeyIcon />}
                />
                <Menu.Item
                    to="/connections"
                    primaryText="connections"
                    leftIcon={<AppShortcutIcon />}
                />
                <Menu.Item
                    to="/security"
                    primaryText="security"
                    leftIcon={<LockIcon />}
                />
            </Menu>
        </>
    );
};

const MyLayout = (props: LayoutProps) => (
    <Layout {...props} appBar={MyAppBar} menu={MyMenu} />
);

export default MyLayout;
