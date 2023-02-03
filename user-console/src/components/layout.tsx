import * as React from 'react';
import {
    AppBar,
    AppBarProps,
    Layout,
    LayoutProps,
    MenuProps,
    useResourceDefinitions,
} from 'react-admin';
import MenuItem from '@mui/material/MenuItem';
import { Box, Typography } from '@mui/material';
import { Menu } from 'react-admin';
import LockIcon from '@mui/icons-material/Lock';

const MyAppBar = (props: AppBarProps) => {
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
                    }}
                >
                    AAC
                </Typography>
            </Box>
        </AppBar>
    );
};

const MyMenu = (props: MenuProps) => {
    const resources = useResourceDefinitions();
    const links = Object.keys(resources)
        .filter(name => resources[name].hasList)
        .map(name => {
            return <Menu.ResourceItem key={name} name={name} />;
        });

    return (
        <Menu>
            <Menu.DashboardItem />
            {links}
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
