import { Link } from 'react-router-dom';
import { useResourceContext, useCreatePath } from 'ra-core';
import { Button, CreateButtonProps } from 'react-admin';
import ContentAdd from '@mui/icons-material/Add';

const defaultIcon = <ContentAdd />;

export const CreateButton = (props: CreateButtonProps) => {
    const {
        className,
        icon = defaultIcon,
        label = 'ra.action.create',
        resource: resourceProp,
        variant,
        ...rest
    } = props;

    const resource = useResourceContext(props);
    const createPath = useCreatePath();

    const path = createPath({ resource, type: 'create' });

    return (
        <Button
            component={Link}
            to={path}
            className={className}
            label={label}
            variant={variant}
            {...(rest as any)}
        >
            {icon}
        </Button>
    );
};

export default CreateButton;
