import {
    AppBar,
    AppBarProps,
    Layout,
    LayoutProps,
    MenuProps,
    useGetIdentity,
} from 'react-admin';
import { Box, Typography } from '@mui/material';
import { Menu } from 'react-admin';

import LockIcon from '@mui/icons-material/Lock';
import AccountBoxIcon from '@mui/icons-material/AccountBox';
import VpnKeyIcon from '@mui/icons-material/VpnKey';
import AppShortcutIcon from '@mui/icons-material/AppShortcut';

const MyAppBar = (props: AppBarProps) => {
    const { data, isLoading } = useGetIdentity();
    let title = 'AAC';
    if (!isLoading && data && data.realm) {
        title = data.realm.name;
    }

    return (
        <AppBar {...props} color="primary">
            <Box flex="1">
                <Typography
                    variant="h6"
                    noWrap
                    component="a"
                    href="/"
                    sx={{
                        mr: 2,
                        display: { xs: 'none', md: 'flex' },
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

    return (
        <Menu>
            <Menu.DashboardItem />
            {/* {links} */}
            <Menu.Item
                to="/accounts"
                primaryText="accounts"
                leftIcon={<LockIcon />}
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
    );
};

const MyLayout = (props: LayoutProps) => (
    <Layout {...props} appBar={MyAppBar} menu={MyMenu} />
);

export default MyLayout;
