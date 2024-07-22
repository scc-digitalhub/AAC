import { useRootSelector } from "@dslab/ra-root-selector";
import { Box, Divider } from "@mui/material";
import { useBasename, useGetResourceLabel, Menu, MenuItemLink } from "react-admin";
import { RealmIcon } from "../myrealms/RealmIcon";
import SettingsIcon from '@mui/icons-material/Settings';

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