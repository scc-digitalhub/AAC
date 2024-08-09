import { useRootSelector } from '@dslab/ra-root-selector';
import {
    Accordion,
    AccordionDetails,
    AccordionSummary,
    Box,
    Divider,
} from '@mui/material';
import {
    useBasename,
    useGetResourceLabel,
    Menu,
    MenuItemLink,
    useTranslate,
    useCreatePath,
} from 'react-admin';
import { RealmIcon } from '../myrealms/RealmIcon';
import SettingsIcon from '@mui/icons-material/Settings';
import { ReactElement } from 'react';
import { AppIcon } from '../apps/AppIcon';
import { UserIcon } from '../users/UserIcon';
import { IdpIcon } from '../idps/IdpIcon';
import { AttributeIcon } from '../attributeset/AttributeIcon';
import { RoleIcon } from '../roles/RoleIcon';

const defaultIcon = <RealmIcon />;

const MenuEntries = (props: {
    children: ReactElement | ReactElement[];
    label: string;
    icon?: ReactElement;
}) => {
    const { label, children, icon = defaultIcon } = props;
    const translate = useTranslate();

    return (
        <Accordion elevation={0}>
            <AccordionSummary>
                <MenuItemLink
                    leftIcon={icon}
                    to={''}
                    // sx={{ textAlign: 'left' }}
                >
                    {translate(label)}
                </MenuItemLink>
            </AccordionSummary>
            <AccordionDetails>{children}</AccordionDetails>
        </Accordion>
    );
};

export const MyMenu = () => {
    const basename = useBasename();
    const getResourceLabel = useGetResourceLabel();
    const { base, root: realmId } = useRootSelector();
    const createPath = useCreatePath();

    return (
        <Menu
            sx={{
                height: '100%',
                pt: '18px',
            }}
        >
            <Box flex={1}>
                <Menu.DashboardItem />
                {/* apps */}
                <Menu.ResourceItem name="apps" />
                <Menu.ResourceItem name="services" />
                {/* users */}
                <Menu.ResourceItem name="users" />
                <Menu.ResourceItem name="groups" />
                {/* authentication */}
                <Menu.ResourceItem name="idps" />
                {/* authorization */}
                <Menu.ResourceItem name="roles" />
                <Menu.ResourceItem name="resources" />
                {/* attributes  */}
                <Menu.ResourceItem name="attributeset" />
                <Menu.ResourceItem name="aps" />

                <Menu.ResourceItem name="audit" />
                <Divider />
                <MenuItemLink
                    leftIcon={<SettingsIcon />}
                    to={createPath({
                        resource: 'myrealms',
                        id: realmId,
                        type: 'edit',
                    })}
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
